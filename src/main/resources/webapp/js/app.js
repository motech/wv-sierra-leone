'use strict';

/* put your routes here */

angular.module('your-module', ['motech-dashboard', 'YourModuleServices', 'ngCookies', 'bootstrap'])
    .config(['$routeProvider', function ($routeProvider) {

        $routeProvider
            .when('/welcome', { templateUrl: '../your-module/partials/welcome.html', controller: YourController })
            .otherwise({redirectTo: '/welcome'});
    }]);
