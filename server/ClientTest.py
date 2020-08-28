import socketio

sio = socketio.Client()

@sio.event
def connect():
    print('Connection successfully')

@sio.event
def disconnect():
    print('Disconnected successfully')

def sendImage(image):
    sio.emit('newImage', image)


sio.connect('http://0.0.0.0:8080') 

sendImage("testing sending images")