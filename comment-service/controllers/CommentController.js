/**
 * Created by Jiravat on 3/31/2017.
 * Modified by Chaniwat (chaniwat.meranote@gmail.com) on 4/28/2017.
 */

const Exception = require('../error/Exception');

const CommentRepository = require('../services/CommentService');
const models = require('../models');

module.exports = {
    createSingle,
    updateSingle,
    getSingle,
    deleteSingle,
    getCommentsByPostId,
    deleteCommentsByPostId,
    countCommentByPostId
};

function* createSingle(req, res) {
    let comment = yield CommentRepository.createOne(req.params.postId, req.query.userId, req.body);
    return res.status(201).json(comment);
}

function* updateSingle(req, res) {
    try {
        let comment = yield CommentRepository.updateOne(req.params.postId, req.params.commentId, req.query.userId, req.body);
        res.status(200).json(comment);
    } catch(e) {
        res.status(e.status).json(e);
    }
}

function* deleteSingle(req, res) {
    try {
        let comment = yield CommentRepository.deleteOne(req.params.postId, req.params.commentId, req.query.userId);
        res.status(200).json(comment);
    } catch(e) {
        res.status(e.status).json(e);
    }
}

function* getSingle(req, res) {
    let comment = yield CommentRepository.getOne(req.params.postId, req.params.commentId);
    if (!comment) {
        res.status(404).json(new Exception("Comment not found", 404))
    } else {
        res.status(200).json(comment);
    }
}

function* getCommentsByPostId(req, res) {
    let comment = yield CommentRepository.getAllByPostId(req.params.postId);
    res.status(200).json(comment);
}

function* deleteCommentsByPostId(req, res) {
    yield CommentRepository.deleteAllByPostId(req.params.postId);
    res.status(200).end();
}

function* countCommentByPostId(req, res) {

}
