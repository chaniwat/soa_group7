<template>
  <div v-bind:class="{'is-active': active}" class="modal">
    <div class="modal-background" v-on:click="hideModal"></div>
    <div class="modal-content">
      <a class="button is-fullwidth is-medium" v-on:click="editComment">Edit Comment</a>
      <a class="button is-fullwidth is-medium" v-on:click="deleteComment">Delete</a>
    </div>
    <button class="modal-close" v-on:click="hideModal"></button>
  </div>
</template>

<script type="text/babel">
  export default {
    props: ['active'],
    data () {
      return {}
    },
    methods: {
      hideModal () {
        this.$emit('hide')
      },
      editComment () {
        this.$emit('edit')
      },
      deleteComment () {
        this.$emit('delete')
      }
    }
  }
</script>
