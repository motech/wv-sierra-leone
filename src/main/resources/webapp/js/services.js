/* put your angular services here */

angular.module('YourModuleServices', ['ngResource'])

    .factory('YourObject', function($resource) {
        return $resource('../sierra-leone/api/your-objects');
});
