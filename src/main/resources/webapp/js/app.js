'use strict';

/* put your routes here */

angular.module('sierra-leone', ['motech-dashboard', 'YourModuleServices', 'ngCookies', 'bootstrap'])
    .config(['$routeProvider', function ($routeProvider) {

        $routeProvider
            .when('/welcome', { templateUrl: '../sierra-leone/partials/welcome.html', controller: YourController })
            .otherwise({redirectTo: '/welcome'});
    }]);
