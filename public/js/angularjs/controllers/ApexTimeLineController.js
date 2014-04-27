 
apexTimeLineControllers.controller('ApexTimeLineController', 
							  ['$scope','ForceAuthService',
  function ($scope,ForceAuthService) {
	$scope.authorized=false;
    $scope.init = function() {
    	$scope.config= CONFIG ;
    };
    


    $scope.initiateOAuth = function (){
    	ForceAuthService.ready($scope.config);
    }

    $scope.oauthCallback=function(popupWindow,tokenHash){
    	popupWindow.close();
    	ForceAuthService.oauthCallback(tokenHash);
    	$scope.init();
    }

    //Register the Oauth callback as a function on the main window
    $window.oauthCallback=$scope.oauthCallback;

  }
]);