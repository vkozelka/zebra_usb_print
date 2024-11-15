var exec = require('cordova/exec');

exports.getDevices = function (zplCode, success, error) {
    exec(success, error, 'zebra_usb_print', 'getDevices', [zplCode]);
};
exports.printZpl = function (zplCode, success, error) {
    exec(success, error, 'zebra_usb_print', 'printZpl', [zplCode]);
};