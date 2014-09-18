<!-- Modal -->
<div class="modal fade" id="editProfileModal" tabindex="-1"
    role="dialog" aria-labelledby="myModalProfileLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" style="font-size:30px">
                    <span aria-hidden="true">&times;</span><span
                        class="sr-only">Close</span>
                </button>
                <h4 class="modal-title" id="myModalProfileLabel">Edit Profile</h4>
            </div>
            <div class="modal-body">

			    <form id="primaryContactForm" class="form-horizontal" role="form">
			
			        <div class="form-group">
			            <div class="alert alert-warning col-sm-offset-1 col-sm-10"
			                id="primaryContactFailed" hidden="true">
			                <a href="#" class="close" data-hide="alert">&times;</a>
			                <div id="primaryContactMessage">Failed saving
			                    details.</div>
			            </div>
			        </div>
			
<!-- 			        <div class="form-group">
			            <label for="email" class="col-sm-3 control-label">Email</label>
			            <div class="col-sm-6">
			                <input type="email" class="form-control" id="email"
			                    name="email" placeholder="janlinv@ubc.ca">
			            </div>
			        </div> -->
			        
			        <div class="form-group">
			            <label for="firstName" class="col-sm-3 control-label">First
			                name</label>
			            <div class="col-sm-8">
			                <input type="text" name="firstName" class="form-control"
			                    id="firstName" placeholder="Jane">
			            </div>
			        </div>
			
			        <div class="form-group">
			            <label for="lastName" class="col-sm-3 control-label">Last
			                name</label>
			            <div class="col-sm-8">
			                <input type="text" name="lastName" class="form-control"
			                    id="lastName" placeholder="Investigator">
			            </div>
			        </div>
			
			        <div class="form-group">
			            <label for="organization" class="col-sm-3 control-label">Organization</label>
			            <div class="col-sm-8">
			                <input type="text" class="form-control"
			                    id="organization" name="organization"
			                    placeholder="University of British Columbia">
			            </div>
			        </div>
			
			        <div class="form-group">
			            <label for="department" class="col-sm-3 control-label">Department</label>
			            <div class="col-sm-8">
			                <input type="text" class="form-control"
			                    name="department" id="department"
			                    placeholder="Department of Zoology">
			            </div>
			        </div>
			
			        <div class="form-group">
			            <label class="col-sm-3 control-label">Website</label>
			            <div class="col-sm-8">
			                <input type="text" class="form-control"
			                    name="website" id="website"
			                    placeholder="http://www.chibi.ubc.ca">
			            </div>
			        </div>
			        
			        <div class="form-group">
			            <label class="col-sm-3 control-label">Phone</label>
			            <div class="col-sm-8">
			                <input type="text" class="form-control"
			                    name="phone" id="phone"
			                    placeholder="604-111-1111">
			            </div>
			        </div>
			        
			        <div class="form-group">
			            <label class="col-sm-3 control-label">Research Focus</label>
			            <div class="col-sm-8">
							<textarea id="description" name="description" class="form-control" rows="3"
								placeholder="My lab studies ..." maxlength="1000"></textarea>
			            </div>
			        </div>
			        
			        <!-- 
			        <div class="form-group">
			            <div class="col-sm-offset-3 col-sm-6">
			                <div class="checkbox">
			                    <label> <input type="checkbox">
			                        Active
			                    </label>
			                </div>
			                <p class="help-block">Lorem ipsum dolor sit amet</p>
			            </div>
			        </div>
			
			        <div class="form-group">
			            <label class="col-sm-3 control-label">Choices</label>
			            <div class="col-sm-6">
			                <div class="checkbox">
			                    <label> <input type="checkbox"
			                        name="optionsRadios" id="optionsCheckbox1"
			                        value="option1" checked> Option one is
			                        this and that&mdash;be sure to include why it's
			                        great
			                    </label>
			                </div>
			                <div class="checkbox">
			                    <label> <input type="checkbox"
			                        name="optionsRadios" id="optionsCheckbox2"
			                        value="option2"> Option two can be
			                        something else and selecting it will deselect
			                        option one
			                    </label>
			                </div>
			            </div>
			        </div>
			
			        <div class="form-group">
			            <label class="col-sm-3 control-label">Sex</label>
			            <div class="col-sm-6">
			                <div class="radio">
			                    <label> <input type="radio"
			                        name="optionsRadios" id="optionsRadios1"
			                        value="option1" checked> Male
			                    </label>
			                </div>
			                <div class="radio">
			                    <label> <input type="radio"
			                        name="optionsRadios" id="optionsRadios2"
			                        value="option2"> Female
			                    </label>
			                </div>
			                <p class="help-block">Lorem ipsum dolor sit amet</p>
			            </div>
			        </div>
			
			 -->
			 		<hr>
					<div class="form-group">
						<div class="col-sm-offset-3 col-sm-8 text-right">
							<button type="button" id="submit" class="btn btn-primary">Save</button>
						</div>
					</div>

			    </form>

            </div>
        </div>
    </div>
</div>



<!-- Our scripts 
<script src="scripts/api/editUser.js"></script>-->