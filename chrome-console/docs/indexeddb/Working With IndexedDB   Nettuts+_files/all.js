<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<meta name="viewport" content="initial-scale = 0.67 maximum-scale = 1, user-scalable=yes, width=device-width" />
	<title>Blocked Domain</title>
	<link rel="stylesheet" href="/static/css/base.css" type="text/css" />
	<script src="/static/js/jquery-1.10.1.min.js"></script>
        <script src="/static/js/jquery.cookie.js"></script>
	<link rel="shortcut icon" type="image/ico" href="/favicon.ico" />
	<script type="text/javascript">
            function show(id) { document.getElementById(id).style.display = ''; }
            function hide(id) { document.getElementById(id).style.display = 'none'; }
            function cookieCheck() {
                $.cookie('bpb_test', 'none');

                // if Get_Cookie succeeds, cookies are enabled, since
                //the cookie was successfully created.
                if ( $.cookie('bpb_test') )
                {
                    $.removeCookie('bpb_test');

                } else {
                    $("#message").addClass("alert alert-error").html('<div><b>Cookies Disabled In Your Browser</b></div> Cookies are required to bypass a block page. Please enable cookies to continue. <a href="https://support.opendns.com/entries/21929725-Block-Page-Bypass-does-not-work-when-cookies-are-not-enabled" target="_BLANK">Learn More</a>');
            	}
            }
            function toggleBypassMask() {
				if(document.getElementById('checkboxToggleBypassMask').checked) {
					//mask the bypass code
					document.getElementById('textBypassCode').style.display = 'none';
					document.getElementById('passwordBypassCode').style.display = '';
					document.getElementById('passwordBypassCode').value = document.getElementById('textBypassCode').value;
				} else {
					//don't mask it
					document.getElementById('textBypassCode').style.display = '';
					document.getElementById('passwordBypassCode').style.display = 'none';
					document.getElementById('textBypassCode').value = document.getElementById('passwordBypassCode').value;
				}
				
				fillBypass(0);
			}
			function fillBypass(submitForm) {
				var bypassCode = '';
				if(document.getElementById('checkboxToggleBypassMask').checked) {
					bypassCode = document.getElementById('passwordBypassCode').value;
				} else {
					bypassCode = document.getElementById('textBypassCode').value;
				}

				document.getElementById('bypassCode').value = bypassCode;
				
				if(submitForm) {
					document.forms['bypassForm'].submit();
				}
			}

    
            function setIframeSrc() {
                var iframe = document.getElementById('contactiframe');
                if (iframe && iframe.src == 'about:blank') {
                    iframe.src = 'https://www.opendns.com/contact/netadmin/?ablock=true&bd=connect.facebook.net&bc=Social Networking&bgcolor=white';
                }
            }
	    function closecontact() {
	        show('toggle-contact');
	        hide('contact');
	    }
	    function hide_wait_icon() {}
	    document.domain = 'opendns.com';
	

            var _gaq = _gaq || [];
            _gaq.push(['_setAccount', 'UA-1163522-16']);
            _gaq.push(['_trackPageview']);

            (function() {
              var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
              ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
              var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
            })();
        </script>
</head>

<body>

<div id="container-top"></div>

<div id="container">
	<div id="container-inner">
		<div class="logo-container">

			<p><img src="https://d2v7u03x06aro3.cloudfront.net/branding-logo/fcb1bc595f901f2b65e82ebe8dcd2c8aa5f2ac4e" /></p>

		</div>

		<h2>This domain is blocked.</h2>




		<p><p><span style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 13px; font-style: normal; font-variant: normal; line-height: 20px;">connect.facebook.net is not allowed on this network. Please email&nbsp;</span><a title="Information Security" href="mailto:isengineering@theice.com" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-style: normal; font-variant: normal; line-height: 20px; font-size: 13px;">Information Security</a><span style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 13px; font-style: normal; font-variant: normal; line-height: 20px;">&nbsp;or open a&nbsp;</span><a title="Footprints" href="https://footprints.cpex.com/" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-style: normal; font-variant: normal; line-height: 20px; font-size: 13px;">Footprints</a><span style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 13px; font-style: normal; font-variant: normal; line-height: 20px;">&nbsp;ticket for further details.</span><br></p></p>



                <p class="light">This site was categorized in: Social Networking</p>


		
		<p style="float: left;">
			<a href="#" onclick="setIframeSrc(); show('contact'); hide('toggle-contact'); return false" id="toggle-contact" class="light">Contact your network administrator</a>
		</p>
		

		<p id="admin-link" style="float: right;">
			<a href="#" onclick="cookieCheck(); show('bypass-login'); hide('admin-link'); return false" class="light">Admin</a>
		</p>

		<div style="clear: both"></div>

		
		<div id="contact" style="display: none; margin: 15px 0 0 0;">
		  <div class="box">
			<iframe id="contactiframe" style="border: 0px; background: #fdf1f1;" frameborder="0" width="100%" height="250px" scrolling="no" src="about:blank"></iframe>
		  </div>
		</div>
		

		<div id="bypass-login" style="display: none; margin: 15px 0 0 0;">
	      
	      	<div class="box">
      	

				<h3 style="margin-bottom: 15px;">Enter your bypass credentials to access this site</h3>
				<div id="message"></div>
				<form method="POST" name="bypassForm">
				
					<div id="credentials-login">
						<h3>Email (or username)</h3>
						<p class="slim"><input type="text" name="username" size="15" /></p>				
						<h3>Password</h3>
						<p><input type="password" name="password" size="15" /><br>
						<a target='_blank' href='http://www.opendns.com/auth/?forgot_password'><b><small>Forgot Your Password?</small></b></a></p>
					</div>

					<div id="or"></div>

					<div id="credentials-code">
						<h3>Bypass Code</h3>
						<p>
							<input type="text" size="15" id="textBypassCode" autocomplete="off" />
							<input type="password" size="15" id="passwordBypassCode" style="display:none;" />
						</p>
						<p id="pMaskBypass"><input type="checkbox" id="checkboxToggleBypassMask" onclick="toggleBypassMask();" /> Mask bypass code</p>
						
						<input type="hidden" name="code" id="bypassCode" />
					</div>
					
					<div style="clear: both"></div>
				
					<p><input class="bypassBtn" type="submit" value="Bypass" onclick="fillBypass(1); return false;" /> or <a href="#" onclick="hide('bypass-login'); show('admin-link'); return false;">cancel</a></p>

					
					<p style="font-size: 13px; color: #999;"><small>Your IP: 8.36.66.4</small></p>
					

				</form>
			</div><!-- /box -->

	      </div><!-- /bypass-login -->

	</div><!-- /container-inner -->
	
</div><!-- /container -->

<div id="container-bottom"></div>



</body>
</html>
