// Empty constructor
function CommunicationServicePlugin() {

}

CommunicationServicePlugin.prototype._pluginInitialize = function() {
  this._isAndroid = device.platform.match(/^android|amazon/i) !== null;
  this.websocketPlugin = cordova.plugins.WebsocketServicePlugin;
  if(!this.websocketPlugin)
    console.log('ATTENZIONE!!!! manca il plugin delle websocket');
  else {
    this.websocketPlugin.on('incomingCall', this.onIncomingCall);
  }
}

CommunicationServicePlugin.prototype.onIncomingCall = function(callObj: any) {
  console.log('GESTIONE CALL', callObj);
}

CommunicationServicePlugin.prototype.initListners = function(overrides: {
  message: (msg: any) => void,
  incomingCall: (msg: any) => void,
  rejectCall: (msg: any) => void,
}) {
 
}

var _defaultListners =
{
    title:   'WebSocketService is running',
    text:    '.......',
    bigText: false,
    resume:  true,
    silent:  false,
    hidden:  true,
    color:   undefined,
    icon:    'ic_launcher'
};

CommunicationServicePlugin.prototype.setDefaults = function(overrides) {

  var defaults = this._defaults;

    for (var key in defaults)
    {
        if (overrides.hasOwnProperty(key))
        {
            defaults[key] = overrides[key];
        }
    }

    if (this._isAndroid)
    {
        cordova.exec(null, null, 'CommunicationServicePlugin', 'configure', [defaults, false]);
    }
}

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'
CommunicationServicePlugin.prototype.show = function(message, duration, successCallback, errorCallback) {
  var options = {};
  options.message = message;
  options.duration = duration;
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'show', [options]);
}

CommunicationServicePlugin.prototype.connect = function(uri, successCallback, errorCallback) {
  var options = {};
  options.uri = uri;
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'connect', [options]);
}

CommunicationServicePlugin.prototype.send = function(msg, successCallback, errorCallback) {
  var options = {};
  options.params = msg;
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'send', [options]);
}

CommunicationServicePlugin.prototype.startBackground = function(successCallback, errorCallback) {
  cordova.plugins.backgroundMode.enable();
}

CommunicationServicePlugin.prototype.checkBackground = function(successCallback, errorCallback) {
  let bool = cordova.plugins.backgroundMode.isActive();
  if(successCallback)
    successCallback(bool);
}

// Installation constructor that binds ToastyPlugin to window
CommunicationServicePlugin.install = function() {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.CommunicationServicePlugin = new CommunicationServicePlugin();
  
  return window.plugins.CommunicationServicePlugin;
};
cordova.addConstructor(CommunicationServicePlugin.install);
