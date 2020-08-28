import eventlet
import socketio

sio = socketio.Server()
app = socketio.WSGIApp(sio)

@sio.event
def connect(sid, environ):
    print('connect', sid)

@sio.event
def newImage(sid, data):
    print(data)

@sio.event
def disconnect(sid):
    print('disconnect', sid)

if __name__ == '__main__':
    eventlet.wsgi.server(eventlet.listen(('', 9000)), app)