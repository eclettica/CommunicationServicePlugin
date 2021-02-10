// Empty constructor
function CommunicationServicePlugin() {
  this._listners = {};
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

CommunicationServicePlugin.prototype.on = function(event, data) {
  console.log('CommunicationService ', event, data);
}

CommunicationServicePlugin.prototype.onEvent = function(eventName, data) {
  console.log('CommunicationService ', eventName, data);
  if(!this._listners[eventName])
    return;
  for (let index = 0; index < this._listners[eventName].length; index++) {
    const element = this._listners[eventName][index];
    if(element)
      element(data);
  }
}

CommunicationServicePlugin.prototype.fireEvent = function(event, data) {
  console.log('CommunicationService ',event, data);
}

CommunicationServicePlugin.prototype.onIncomingCall = function(callObj) {
  console.log('GESTIONE CALL', callObj);
}

CommunicationServicePlugin.prototype.loadUsers = function(successCallback, errorCallback) {
  var options = {};
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'loadUsers', [options]);
}

CommunicationServicePlugin.prototype.reloadUsers = function(successCallback, errorCallback) {
  var options = {};
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'reloadUsers', [options]);
}

CommunicationServicePlugin.prototype.addListner = function(eventName, listner) {
  if(!eventName || !listner)
    return;
 if(!this._listners[eventName]) {
   this._listners[eventName] = [];
 }
 if(!this._listners[eventName].includes(listner))
    this._listners[eventName].push(listner)
}

CommunicationServicePlugin.prototype.removeListner =function(eventName, listner) {
  if(!eventName || !listner)
    return;
  if(!this._listners[eventName])
    return;
  var idx = this._listners[eventName].indexOf(listner);
  if(idx > -1)
    this._listners[eventName].splice(idx, 1);
}

var _listners =
{
    
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

CommunicationServicePlugin.prototype.connect = function(userId, successCallback, errorCallback) {
  var options = {};
  options.userId = userId;
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'connect', [options]);
}

CommunicationServicePlugin.prototype.startService = function(uri, successCallback, errorCallback) {
  var options = {};
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'startService', [options]);
}

CommunicationServicePlugin.prototype.addMessage = function(message, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'addMessage', [message]);
}

CommunicationServicePlugin.prototype.getMessages = function(options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'getMessages', [options]);
}

CommunicationServicePlugin.prototype.getChats = function(options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'CommunicationServicePlugin', 'getChats', [options]);
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
