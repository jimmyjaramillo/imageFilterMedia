var exec = require('cordova/exec');

exports.getImageByGeoDate = function(args, success, error) {
  var params = {
    Radius: args.radius ? args.radius: null,
    Latitude: args.latitude ? args.latitude: null,
    Longitude: args.longitude ? args.longitude: null,
    DataTimeStart: args.dataTimeStart ? args.dataTimeStart: null,
    DataTimeFinish: args.dataTimeFinish ? args.dataTimeFinish: null
  };
    exec(success, error, "imageFilterMedia", "getImageByGeoDate", [params]);
};
