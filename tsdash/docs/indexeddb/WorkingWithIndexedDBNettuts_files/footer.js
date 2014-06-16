jQuery(document).ready(function($) {

  /* sidebar tabs */
  $(function(){
    $("#site-updates").tabs();
    $("#social-networking").tabs();
    $('#facebook-section').html('<fb:like-box href="' + Env.facebookUrl + '" width="388" height="290" stream="false"></fb:like-box>');
  });

  /* comment-ribbon */
  $(function(){
    $('.is-staff, .is-author').addClass('highlighted').children('div').wrapInner('<div class="wrap-comment" />');
  });
  
  /* site switcher */
  $(function(){
    $('.site_switcher').click(function(e) {
      $(this).toggleClass('active');
      e.stopPropagation();
    });
    $(document).bind('click', function(e) {
      if (!$(e.target).parents().hasClass('site_switcher')) {
        $('.site_switcher').removeClass('active');
      }
    });
  });
  
  /* session list text */
  $(function(){
    var list_count = $('.single-post .session-list ul').children().length;
    if ( list_count > 1 ) {
      $('.single-post .session-post em').append(' - <a href="javascript:void(0);" class="show-all">Show All</a>');
    }
    $('a.show-all').click(function() {
      $('.session-list').toggle();
    });
  });
  
  /* site switcher */
  $(function(){
    $('.envato-item-tab > a').click(function(e) {
      e.stopPropagation();
      e.preventDefault();
      
      var ID = $(this).attr('href');
      
      $('.envato-item-tab, .envato-item-container').removeClass('active');
      $(ID).toggleClass('active');
      $(this).parent('li').toggleClass('active');
      
    });
  });

});