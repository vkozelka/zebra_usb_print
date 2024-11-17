var exec = require('cordova/exec');

exports.getDevice = function (success, error) {
    exec(success, error, 'zebra_usb_print', 'getDevice', []);
};
exports.printZpl = function (zplCode, success, error) {
    exec(success, error, 'zebra_usb_print', 'printZpl', [zplCode]);
};