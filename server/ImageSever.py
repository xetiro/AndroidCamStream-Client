import eventlet
import socketio
import base64
import cv2
import numpy as np

sio = socketio.Server()
app = socketio.WSGIApp(sio)

@sio.event
def connect(sid, environ):
    print('connect', sid)

@sio.event
def newImage(sid, data):
    save(data)


@sio.event
def disconnect(sid):
    print('disconnect', sid)

def save(byteArray):
    print(byteArray)

if __name__ == '__main__':
    eventlet.wsgi.server(eventlet.listen(('', 9000)), app)