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
					<div class="col-md-9">
						<div class="user-info-right">
							<div class="basic-info">
								<h3>
									<a href="#"><i class="fa fa-edit"></i></a> Basic Information
								</h3>
								<p class="data-row">
									<span class="data-name">First Name</span> <span
										class="data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Last Name</span> <span
										class="data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Organization</span> <span
										class="data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Department</span> <span
										class="data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Website</span> <span class="data-value link"></span>
								</p>
							</div>
							<div class="contact-info">
								<h3>
									<a href="#"><i class="fa fa-edit"></i></a> Contact Information
								</h3>
								<p class="data-row">
									<span class="data-name">Email</span> <span class="locked data-value"></span>
								</p>
								<p class="data-row">
									<span class="data-name">Phone</span> <span class="data-value"></span>
								</p>
							</div>
							<div class="about">
								<h3>
									<a href="#"><i class="fa fa-edit"></i></a> About My Research
								</h3>
								<div class="col-sm-8">
									<p class="data-paragraph"></p>
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

	<div class="modal fade" id="profileModal" tabindex="-1" role="dialog"
		aria-labelledby="profileModalLabel" aria-hidden="true">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal"
						style="font-size: 30px">
						<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
					</button>
					<h4 class="modal-title" id="profileModalLabel">Edit Profile</h4>
				</div>
				<div class="modal-body">

					<form class="form-horizontal" role="form">
						<div class="form-group">
							<label for="firstName" class="col-sm-3 control-label">First
								name</label>
							<div class="col-sm-8">
								<input type="text" name="basic-info" class="form-control"
									placeholder="Jane">
							</div>
						</div>

						<div class="form-group">
							<label for="lastName" class="col-sm-3 control-label">Last
								name</label>
							<div class="col-sm-8">
								<input type="text" name="basic-info" class="form-control"
									placeholder="Investigator">
							</div>
						</div>

						<div class="form-group">
							<label for="organization" class="col-sm-3 control-label">Organization</label>
							<div class="col-sm-8">
								<input type="text" class="form-control" name="basic-info"
									placeholder="University of British Columbia">
							</div>
						</div>

						<div class="form-group">
							<label for="department" class="col-sm-3 control-label">Department</label>
							<div class="col-sm-8">
								<input type="text" class="form-control" name="basic-info"
									placeholder="Department of Zoology">
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-3 control-label">Website</label>
							<div class="col-sm-8">
								<input type="text" class="form-control" name="basic-info"
									placeholder="http://www.chibi.ubc.ca">
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-3 control-label">Phone</label>
							<div class="col-sm-8">
								<input type="text" class="form-control" name="contact-info"
									placeholder="604-111-1111">
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-3 control-label">Research Focus</label>
							<div class="col-sm-8">
								<textarea name="about" class="form-control" rows="3"
									placeholder="My lab studies ..." maxlength="1200"></textarea>
							</div>
						</div>

					</form>

				</div>
				<div class="modal-footer">
					<div class="form-group">
						<div class="col-sm-offset-7 col-sm-2">
							<button type="button" class="btn btn-default"
								data-dismiss="modal">Submit</button>
						</div>
						<div class="col-sm-2">
							<button type="button" class="btn btn-default"
								data-dismiss="modal">Cancel</button>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>


</div>