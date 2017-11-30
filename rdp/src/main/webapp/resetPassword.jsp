<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
<meta charset="UTF-8" />
<title>${ SETTINGS["rdp.fullname"]}</title>

<!-- Bootstrap core CSS -->
<link href="styles/bootstrap.min.css" rel="stylesheet">

<!-- Optional Bootstrap Theme -->
<link href="data:text/css;charset=utf-8,"
    data-href="styles/bootstrap-theme.min.css" rel="stylesheet"
    id="bs-theme-stylesheet">

<style>
	/*Lighten placeholder text*/
	.form-control::-moz-placeholder {
	  color: #A9A9A9;
	  opacity: 1;
	}
	.form-control:-ms-input-placeholder {
	  color: #A9A9A9;
	}
	.form-control::-webkit-input-placeholder {
	  color: #A9A9A9;
	}
</style>

</head>
<body>


    <!-- Modal -->
    <div class="modal bs-modal-sm" id="resetPasswordModal" tabindex="-1"
        role="dialog" aria-labelledby="mySmallModalLabel"
        aria-hidden="true">
        <div class="modal-dialog" data-backdrop="static"
            data-show="true">
            <div class="modal-content" data-backdrop="static" data-show="true">
	            <div class="modal-header">
					<h4 class="modal-title text-center"><a href="${pageContext.request.contextPath}/login.jsp" >${ SETTINGS["rdp.fullname"]}</a></h4>
					<h5 class="modal-title text-center">Password Reset</h5>
	            </div>
                <div class="modal-body">
                	<form id="resetPasswordForm" class="form-horizontal" role="form">
						<div class="alert alert-warning col-sm-offset-1 col-sm-10"hidden="true">
							<a href="#" class="close" data-hide="alert">&times;</a>
							<div class="text-center"></div>
						</div>
						
						<div class="form-group" hidden="true">
				            <label for="user" class="col-sm-4 control-label"></label>
				            <div class="col-sm-7">
				                <input type="text" name="user" class="form-control" id="user">
				            </div>
				        </div>
				        
						<div class="form-group" hidden="true">
				            <label for="key" class="col-sm-4 control-label"></label>
				            <div class="col-sm-7">
				                <input type="text" name="key" class="form-control" id="key">
				            </div>
				        </div>
						
				        <div class="form-group">
				            <label for="password" class="col-sm-4 control-label">New password:</label>
				            <div class="col-sm-7">
				                <input type="password" name="password" class="form-control"
				                    id="password" placeholder="********" required>
				            </div>
				        </div>
				        
				        <div class="form-group">
				            <label for="passwordConfirm" class="col-sm-4 control-label">Confirm Password:</label>
				            <div class="col-sm-7">
				                <input type="password" name="passwordConfirm" class="form-control"
				                    id="passwordConfirm" placeholder="********" required>
				            </div>
				        </div>
		
						<div class="form-group">
							<div class="col-sm-offset-3 col-sm-8 text-right">
								<button type="submit" id="btnSubmit" class="btn btn-success">Submit</button>
							</div>
						</div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- include jQuery, jQuery UI, and our script file -->
    <script src="scripts/lib/jquery-1.11.1.js"></script>
    <script src="scripts/lib/bootstrap.min.js"></script>
    <script src="scripts/lib/jquery.validate.min.js"></script>

    <!-- Our scripts -->
    <script src="scripts/api/utility.js?version=1"></script>
    <script src="scripts/api/resetPassword.js?version=1"></script>
    
</body>
</html>
