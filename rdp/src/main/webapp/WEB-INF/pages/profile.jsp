<div>
	<div class="main-header">
		<h2>Profile</h2>
		<em>user profile page</em>
	</div>

	<div class="main-content">
		<!-- NAV TABS -->
		<ul class="nav nav-tabs">
			<li class="active"><a href="#profile-tab" data-toggle="tab"><i
					class="fa fa-user"></i> Profile</a></li>
			<li class=""><a href="#settings-tab" data-toggle="tab"><i
					class="fa fa-gear"></i> Settings</a></li>
		</ul>
		<!-- END NAV TABS -->

		<div class="tab-content profile-page">
			<!-- PROFILE TAB CONTENT -->
			<div class="tab-pane profile active" id="profile-tab">
				<div class="row">
					<div class="col-md-12">
						<div class="user-info-left col-md-4">
							<div class="basic-info">
								<h3>
									<a href="#"><i class="fa fa-edit yellow-icon"></i></a> Basic Information
								</h3>
								<p class="data-row">
									<span class="data-name">First Name</span> <span
										class="data-editable data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Last Name</span> <span
										class="data-editable data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Organization</span> <span
										class="data-editable data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Department</span> <span
										class="data-editable data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Website</span> <span class="data-editable data-value link"></span>
								</p>
							</div>
							<div class="contact-info">
								<h3>
									<a href="#"><i class="fa fa-edit yellow-icon"></i></a> Contact Information
								</h3>
								<p class="data-row">
									<span class="data-name">Email</span> <span class="data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Phone</span> <span class="data-editable data-value"></span>
								</p>
							</div>

						</div>
						<div class="user-info-right col-md-8">
							<div class="about col-sm-12">
								<h3>
									<a href="#"><i class="fa fa-edit yellow-icon"></i></a> About My Research
								</h3>
								<p custom-placeholder=true data-ph="My lab studies ..." class="data-editable data-paragraph"></p>
							</div>
							<div class="publications col-sm-3">
								<h3>
									<a href="#"><i class="fa fa-edit yellow-icon"></i></a> Publications
								</h3>
								<div id="publicationsList">
								<ul class="data-editable">								
								</ul>
								</div>
							</div>
						</div>
					</div>
					
				</div>
				<br>
				<div class="col-md-9">
					<button class="btn btn-danger">
						<i class="fa fa-floppy-o"></i> Save Changes
					</button>
				</div>
			</div>
			<!-- END PROFILE TAB CONTENT -->

			<!-- SETTINGS TAB CONTENT -->
			<div class="tab-pane settings" id="settings-tab">
				<form class="form-horizontal" role="form">
					<fieldset>
						<h3>
							<i class="fa fa-key"></i> Change Password
						</h3>
						<div class="form-group">
							<label for="oldPassword" class="col-sm-3 control-label">Old
								Password</label>
							<div class="col-sm-4">
								<input type="password" id="oldPassword" name="oldPassword"
									class="form-control">
							</div>
						</div>
						<hr>
						<div class="form-group">
							<label for="password" class="col-sm-3 control-label">New
								Password</label>
							<div class="col-sm-4">
								<input type="password" id="password" name="password"
									class="form-control">
							</div>
						</div>
						<div class="form-group">
							<label for="passwordConfirm" class="col-sm-3 control-label">Repeat
								Password</label>
							<div class="col-sm-4">
								<input type="password" id="passwordConfirm" name="passwordConfirm"
									class="form-control">
							</div>
						</div>
					</fieldset>

				</form>
				<br>
				<div class="col-md-9">
					<button class="btn btn-danger">
						<i class="fa fa-floppy-o"></i> Save Changes
					</button>
				</div>



			</div>
			<!-- END SETTINGS TAB CONTENT -->
		</div>

	</div>
	<!-- /main-content -->

</div>