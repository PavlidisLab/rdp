<div class="container">

    <div class="alert alert-warning col-sm-offset-3 col-sm-6"
        id="listResearchersFailed" hidden="true">
        <a href="#" class="close" data-dismiss="alert">&times;</a>
        <div id="listResearchersMessage">Failed loading all researchers.</div>
    </div>

    <table id="listResearchersTable"
        class="table table-striped table-bordered" cellspacing="0"
        width="100%">
        <thead>
            <tr>
                <th>Username</th>
                <th>Email</th>
                <th>First name</th>
                <th>Last name</th>
                <th>Organization</th>
                <th>Department</th>
            </tr>
        </thead>

        <tfoot>
            <tr>
                <th>Username</th>
                <th>Email</th>
                <th>First name</th>
                <th>Last name</th>
                <th>Organization</th>
                <th>Department</th>
            </tr>
        </tfoot>

        <tbody>
            <!--  
        <tr>
                <td>testUsername</td>
                <td>testEmail</td>
                <td>testFirstname</td>
                <td>testLastname</td>
                <td>testOrganization</td>
                <td>testDepartment</td>
            </tr>
        -->
        </tbody>
    </table>
</div>

<!-- Our scripts -->
<script src="scripts/api/listResearchers.js"></script>