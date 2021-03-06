package com.shenzhentagram;

import com.shenzhentagram.errors.NotificationNotFoundException;
import com.shenzhentagram.errors.NotificationNotModifiedException;
import com.shenzhentagram.errors.NotificationTypeNotFoundException;
import com.shenzhentagram.mappers.NotificationPostRowMapper;
import com.shenzhentagram.mappers.NotificationReactionRowMapper;
import com.shenzhentagram.mappers.NotificationRowMapper;
import com.shenzhentagram.mappers.NotificationUserRowMapper;
import com.shenzhentagram.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Repository
public class NotificationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected Log log = LogFactory.getLog(this.getClass());

    @Transactional(readOnly = true)
    public List<Notification> findByUserId(Long id, int limit, int page) {
        try {
            List<Notification> notifications = this.jdbcTemplate.query(
                    "SELECT id, userId, type_, time, text, thumbnail, notificationId, checkStatus " +
                            "FROM notifications " +
                            "WHERE userId = ? ORDER BY time DESC LIMIT ? OFFSET ?",
                    new Object[]{
                            id,
                            limit,
                            limit * page
                    },
                    new NotificationRowMapper()
            );
            notifications.forEach(notification -> {
                switch (notification.getType()) {
                    case "followed_by":
                        notification.setFrom(findNotificationUserById(notification.getNotificationId()));
                        break;
                    case "comment":
                        notification.setFrom(findNotificationPostById(notification.getNotificationId()));
                        break;
                    case "reaction":
                        notification.setFrom(findNotificationReactionById(notification.getNotificationId()));
                        break;
                    default:
                        throw new NotificationTypeNotFoundException(notification.getType());
                }
            });
            return notifications;
        } catch (Exception exception) {
            log.error(exception);
            throw new NotificationNotFoundException(id);
        }
    }

    @Transactional(readOnly = true)
    public Notification findById(Long id) {
        try {
            Notification notification = this.jdbcTemplate.queryForObject(
                    "SELECT id, userId, type_, text, time, thumbnail, notificationId, checkStatus " +
                            "FROM notifications " +
                            "WHERE id = ?",
                    new Object[]{
                            id
                    },
                    new NotificationRowMapper()
            );
            switch (notification.getType()) {
                case "followed_by":
                    notification.setFrom(findNotificationUserById(notification.getNotificationId()));
                    break;
                case "comment":
                    notification.setFrom(findNotificationPostById(notification.getNotificationId()));
                    break;
                case "reaction":
                    notification.setFrom(findNotificationReactionById(notification.getNotificationId()));
                    break;
                default:
                    throw new NotificationTypeNotFoundException(notification.getType());
            }
            return notification;
        } catch (Exception exception) {
            log.error(exception);
            throw new NotificationNotFoundException(id);
        }
    }

    @Transactional
    public void delete(Long id) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        this.jdbcTemplate.update(sql, id);
    }

    @Transactional(readOnly = true)
    public NotificationPost findNotificationPostById(Long id) {
        try {
            NotificationPost notification = this.jdbcTemplate.queryForObject(
                    "SELECT id, post_id, comment_id " +
                            "FROM notificationPosts " +
                            "WHERE id = ?",
                    new Object[]{
                            id
                    },
                    new NotificationPostRowMapper()
            );
            return notification;
        } catch (Exception exception) {
            log.error(exception);
            throw new NotificationNotFoundException(id);
        }
    }

    @Transactional(readOnly = true)
    public NotificationUser findNotificationUserById(Long id) {
        try {
            NotificationUser notification = this.jdbcTemplate.queryForObject(
                    "SELECT id, userId " +
                            "FROM notificationUsers " +
                            "WHERE id = ?",
                    new Object[]{
                            id
                    },
                    new NotificationUserRowMapper()
            );
            return notification;
        } catch (Exception exception) {
            log.error(exception);
            throw new NotificationNotFoundException(id);
        }
    }

    @Transactional(readOnly = true)
    public NotificationReaction findNotificationReactionById(Long id) {
        try {
            NotificationReaction notification = this.jdbcTemplate.queryForObject(
                    "SELECT id, reaction_id " +
                            "FROM notificationReactions " +
                            "WHERE id = ?",
                    new Object[]{
                            id
                    },
                    new NotificationReactionRowMapper()
            );
            return notification;
        } catch (Exception exception) {
            log.error(exception);
            throw new NotificationNotFoundException(id);
        }
    }

    public int createNotifications(List<Notification> notifications) {
        try {
            String insertSql = "insert into notifications(id,userId ,type_,text,thumbnail,notificationId ,checkStatus) values(?,? ,?,?,?,? ,?) " +
                    "ON DUPLICATE KEY UPDATE userId=? ,type_=?,text=?,thumbnail=?,notificationId=?,checkStatus=?";
            for (Notification notification : notifications) {
                long notificationId;
                switch (notification.getType()) {
                    case "followed_by":
                        notificationId = createNotificationUser((NotificationUser) notification.getFrom());
                        break;
                    case "comment":
                        notificationId = createNotificationPost((NotificationPost) notification.getFrom());
                        break;
                    case "reaction":
                        notificationId = createNotificationReaction((NotificationReaction) notification.getFrom());
                        break;
                    default:
                        throw new NotificationTypeNotFoundException(notification.getType());
                }
                notification.setNotificationId(notificationId);
                this.jdbcTemplate.update(insertSql,
                        new Object[]{
                                notification.getId(),
                                notification.getUserId(),
                                notification.getType(),
                                notification.getText(),
                                notification.getThumbnail(),
                                notification.getNotificationId(),
                                notification.getCheckStatus(),
                                notification.getUserId(),
                                notification.getType(),
                                notification.getText(),
                                notification.getThumbnail(),
                                notification.getNotificationId(),
                                notification.getCheckStatus()
                        }
                );
            }
            return HttpServletResponse.SC_CREATED;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

    @Transactional()
    public long createNotificationUser(NotificationUser notificationUser) {
        try {
            String insertSql = "insert into notificationUsers(userId) values(?) ON DUPLICATE KEY UPDATE userId=?";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            this.jdbcTemplate.update(new PreparedStatementCreator() {
                                         @Override
                                         public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                                             PreparedStatement statement = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                                             statement.setLong(1, notificationUser.getUserId());
                                             statement.setLong(2, notificationUser.getUserId());
                                             return statement;
                                         }
                                     }, keyHolder
            );
            return keyHolder.getKey().longValue();
        } catch (Exception exception) {
            throw exception;
        }
    }

    @Transactional()
    public long createNotificationPost(NotificationPost notificationPost) {
        try {
            String insertSql = "insert into notificationPosts(post_id ,comment_id) values(? ,?) ON DUPLICATE KEY UPDATE post_id=?, comment_id=?;";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            this.jdbcTemplate.update(new PreparedStatementCreator() {
                                         @Override
                                         public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                                             PreparedStatement statement = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                                             statement.setLong(1, notificationPost.getPost_id());
                                             statement.setString(2, notificationPost.getComment_id());
                                             statement.setLong(3, notificationPost.getPost_id());
                                             statement.setString(4, notificationPost.getComment_id());
                                             return statement;
                                         }
                                     },
                    keyHolder
            );
            return keyHolder.getKey().longValue();
        } catch (Exception exception) {
            throw exception;
        }
    }

    @Transactional()
    public long createNotificationReaction(NotificationReaction notificationReaction) {
        try {
            String insertSql = "insert into notificationReactions(reaction_id) values(?) ON DUPLICATE KEY UPDATE reaction_id=?;";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            this.jdbcTemplate.update(new PreparedStatementCreator() {
                                         @Override
                                         public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                                             PreparedStatement statement = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                                             statement.setLong(1, notificationReaction.getReaction_id());
                                             statement.setLong(2, notificationReaction.getReaction_id());
                                             return statement;
                                         }
                                     }
                    , keyHolder
            );
            return keyHolder.getKey().longValue();
        } catch (Exception exception) {
            throw exception;
        }
    }

    @Transactional
    public int updateNotificationsStatus(List<Integer> notificationIds, int status) {
        try {
            String insertSql = "UPDATE notifications SET checkStatus = ? " +
                    "WHERE id = ?";

            for (int id : notificationIds) {
                this.jdbcTemplate.update(insertSql,
                        new Object[]{
                                status,
                                id
                        }
                );
            }

            return HttpServletResponse.SC_OK;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

    @Transactional()
    public int updateNotifications(List<Notification> notifications) {
        try {
            String insertSql = "insert into notifications(id,userId ,type_,text,thumbnail,notificationId ,checkStatus) values(?,? ,?,?,?,? ,?) " +
                    "ON DUPLICATE KEY UPDATE userId=? ,type_=?,text=?,thumbnail=?,notificationId=?,checkStatus=?";

            for (Notification notification : notifications) {
                int notificationResponse;
                switch (notification.getType()) {
                    case "followed_by":
                        notificationResponse = updateNotificationUser((NotificationUser) notification.getFrom());
                        break;
                    case "comment":
                        notificationResponse = updateNotificationPost((NotificationPost) notification.getFrom());
                        break;
                    case "reaction":
                        notificationResponse = updateNotificationReaction((NotificationReaction) notification.getFrom());
                        break;
                    default:
                        throw new NotificationTypeNotFoundException(notification.getType());
                }
                this.jdbcTemplate.update(insertSql,
                        new Object[]{
                                notification.getId(),
                                notification.getUserId(),
                                notification.getType(),
                                notification.getText(),
                                notification.getThumbnail(),
                                notification.getFrom().getId(),
                                notification.getCheckStatus(),
                                notification.getUserId(),
                                notification.getType(),
                                notification.getText(),
                                notification.getThumbnail(),
                                notification.getFrom().getId(),
                                notification.getCheckStatus()
                        }
                );
                if (notificationResponse == HttpServletResponse.SC_NOT_MODIFIED) {
                    throw new NotificationNotModifiedException();
                }
            }


            return HttpServletResponse.SC_OK;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

    @Transactional()
    public int updateNotificationUser(NotificationUser notificationUser) {
        try {
            String insertSql = "insert into notificationUsers(id, userId) values(?, ?) ON DUPLICATE KEY UPDATE userId=?";
            this.jdbcTemplate.update(insertSql,
                    new Object[]{
                            notificationUser.getId(),
                            notificationUser.getUserId(),
                            notificationUser.getUserId()
                    }
            );
            return HttpServletResponse.SC_OK;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

    @Transactional()
    public int updateNotificationPost(NotificationPost notificationPost) {
        try {
            String insertSql = "insert into notificationPosts(id,post_id ,comment_id) values(?, ? ,?) ON DUPLICATE KEY UPDATE post_id=?, comment_id=?;";
            this.jdbcTemplate.update(insertSql,
                    new Object[]{
                            notificationPost.getId(),
                            notificationPost.getPost_id(),
                            notificationPost.getComment_id(),
                            notificationPost.getPost_id(),
                            notificationPost.getComment_id()
                    }
            );
            return HttpServletResponse.SC_OK;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

    @Transactional()
    public int updateNotificationReaction(NotificationReaction notificationReaction) {
        try {
            String insertSql = "insert into notificationReactions(id,reaction_id) values(?,?) ON DUPLICATE KEY UPDATE reaction_id=?;";
            this.jdbcTemplate.update(insertSql,
                    new Object[]{
                            notificationReaction.getId(),
                            notificationReaction.getReaction_id(),
                            notificationReaction.getReaction_id()
                    }
            );
            return HttpServletResponse.SC_OK;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

    @Transactional()
    public int updateNotification(Notification notification, long notificationId) {
        try {
            String insertSql = "insert into notifications(id,userId ,type_,text,thumbnail,notificationId ,checkStatus) values(?,? ,?,?,?,? ,?) " +
                    "ON DUPLICATE KEY UPDATE userId=? ,type_=?,text=?,thumbnail=?,notificationId=?,checkStatus=?";

            int notificationResponse;
            switch (notification.getType()) {
                case "followed_by":
                    notificationResponse = updateNotificationUser((NotificationUser) notification.getFrom());
                    break;
                case "comment":
                    notificationResponse = updateNotificationPost((NotificationPost) notification.getFrom());
                    break;
                case "reaction":
                    notificationResponse = updateNotificationReaction((NotificationReaction) notification.getFrom());
                    break;
                default:
                    throw new NotificationTypeNotFoundException(notification.getType());
            }
            this.jdbcTemplate.update(insertSql,
                    new Object[]{
                            notificationId,
                            notification.getUserId(),
                            notification.getType(),
                            notification.getText(),
                            notification.getThumbnail(),
                            notification.getFrom().getId(),
                            notification.getCheckStatus(),
                            notification.getUserId(),
                            notification.getType(),
                            notification.getText(),
                            notification.getThumbnail(),
                            notification.getFrom().getId(),
                            notification.getCheckStatus()
                    }
            );
            if (notificationResponse == HttpServletResponse.SC_NOT_MODIFIED) {
                throw new NotificationNotModifiedException();
            }

            return HttpServletResponse.SC_OK;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

    @Transactional
    public int createNotifications(List<Notification> notifications, String type) {
        try {
            String insertSql = "insert into notifications(id,userId ,type_,text,thumbnail,notificationId ,checkStatus) values(?,? ,?,?,?,? ,?) " +
                    "ON DUPLICATE KEY UPDATE userId=? ,type_=?,text=?,thumbnail=?,notificationId=?,checkStatus=?";
            for (Notification notification : notifications) {
                long notificationId;
                switch (type) {
                    case "followed_by":
                        notificationId = createNotificationUser((NotificationUser) notification.getFrom());
                        break;
                    case "comment":
                        notificationId = createNotificationPost((NotificationPost) notification.getFrom());
                        break;
                    case "reaction":
                        notificationId = createNotificationReaction((NotificationReaction) notification.getFrom());
                        break;
                    default:
                        throw new NotificationTypeNotFoundException(type);
                }
                notification.setNotificationId(notificationId);
                this.jdbcTemplate.update(insertSql,
                        new Object[]{
                                notification.getId(),
                                notification.getUserId(),
                                notification.getType(),
                                notification.getText(),
                                notification.getThumbnail(),
                                notification.getNotificationId(),
                                notification.getCheckStatus(),
                                notification.getUserId(),
                                notification.getType(),
                                notification.getText(),
                                notification.getThumbnail(),
                                notification.getNotificationId(),
                                notification.getCheckStatus()
                        }
                );
            }

            return HttpServletResponse.SC_CREATED;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

    int createNotification(Notification notification) {
        try {
            String insertSql = "insert into notifications(id,userId ,type_,text,thumbnail,notificationId ,checkStatus) values(?,? ,?,?,?,? ,?) " +
                    "ON DUPLICATE KEY UPDATE userId=? ,type_=?,text=?,thumbnail=?,notificationId=?,checkStatus=?";
            long notificationId;
            switch (notification.getType()){
                case "followed_by":
                    notificationId = createNotificationUser((NotificationUser) notification.getFrom());
                    break;
                case "comment":
                    notificationId = createNotificationPost((NotificationPost) notification.getFrom());
                    break;
                case "reaction":
                    notificationId = createNotificationReaction((NotificationReaction) notification.getFrom());
                    break;
                default:
                    throw new NotificationTypeNotFoundException(notification.getType());
            }
            notification.setNotificationId(notificationId);
            this.jdbcTemplate.update(insertSql,
                    new Object[]{
                            notification.getId(),
                            notification.getUserId(),
                            notification.getType(),
                            notification.getText(),
                            notification.getThumbnail(),
                            notification.getNotificationId(),
                            notification.getCheckStatus(),
                            notification.getUserId(),
                            notification.getType(),
                            notification.getText(),
                            notification.getThumbnail(),
                            notification.getNotificationId(),
                            notification.getCheckStatus()
                    });

            return HttpServletResponse.SC_CREATED;
        } catch (Exception exception) {
            log.error(exception);
            return HttpServletResponse.SC_NOT_MODIFIED;
        }
    }

}