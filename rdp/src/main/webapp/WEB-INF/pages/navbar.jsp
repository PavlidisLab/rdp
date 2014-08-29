<nav id="navbar" class="navbar navbar-default" role="navigation">
    <div class="container-fluid">
        <!-- Brand and toggle get grouped for better mobile display -->
        <div class="navbar-header">
            <button type="button" class="navbar-toggle"
                data-toggle="collapse"
                data-target="#bs-example-navbar-collapse-1">
                <span class="sr-only">Toggle navigation</span> <span
                    class="icon-bar"></span> <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Rare Disease Project</a>
        </div>

        <!-- Collect the nav links, forms, and other content for toggling -->
        <!-- 
    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
      <ul class="nav navbar-nav">
        <li class="active"><a href="#">Link</a></li>
        <li><a href="#">Link</a></li>
        <li class="dropdown">
          <a href="#" class="dropdown-toggle" data-toggle="dropdown">Dropdown <span class="caret"></span></a>
          <ul class="dropdown-menu" role="menu">
            <li><a href="#">Action</a></li>
            <li><a href="#">Another action</a></li>
            <li><a href="#">Something else here</a></li>
            <li class="divider"></li>
            <li><a href="#">Separated link</a></li>
            <li class="divider"></li>
            <li><a href="#">One more separated link</a></li>
          </ul>
        </li>
      </ul>
     -->

        <!-- right navbar -->
        <ul class="nav navbar-nav navbar-right">
            <!-- 
        <li><a href="#" id="navbarWelcome">Welcome!</a></li>
             -->

            <div id="navbarIsAdmin" hidden="true">false</div>
            
            <!-- username will be added here when the user has logged in -->
            <li class="dropdown"><a href="#"
                class="dropdown-toggle" data-toggle="dropdown" id="navbarUsername">
            </a>
            
                <ul class="dropdown-menu" role="menu">
                    <li><a href="#changePasswordModal"
                        data-toggle="modal">Change password</a></li>
                    <li><a href="#editProfileModal"
                        data-toggle="modal">Edit Profile</a></li>
                    <li class="divider"></li>
                    <li><a id="logout">Logout</a></li>
                </ul></li>
        </ul>
    </div>
    <!-- /.navbar-collapse -->
    </div>
    <!-- /.container-fluid -->
</nav>



<!-- User signup -->
<%@ include file="editUser.jsp"%>
<!-- User Profile Edit -->
<%@ include file="editProfile.jsp"%>

<!-- include jQuery, and our script file -->
<script src="scripts/lib/jquery-1.11.1.min.js"></script>
<script src="scripts/lib/bootstrap.min.js"></script>
<script src="scripts/lib/jquery.validate.min.js"></script>

<!-- inlcude our script -->
<script src="scripts/api/navbar.js"></script>