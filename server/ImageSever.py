# This is a simple testing server to receive stream of pictures from
# the AndroidTVCameraToServer app. 
#
# Created by xetiro (aka Ruben Geraldes) on 28/09/2020.
import eventlet
import socketio
import cv2
import numpy as np
import base64
from engineio.payload import Payload

# Default was 16 which can create a bootleneck for video streaming
Payload.max_decode_packets = 64

sio = socketio.Server()
app = socketio.WSGIApp(sio)

@sio.event
def connect(sid, environ):
    print('connect', sid)
    

@sio.event
def newPicture(sid, data):
    #print(len(data))
    show(sid, data)

@sio.event
def disconnect(sid):
    print('disconnect', sid)

def show(sid, img_bytes):
    nparr = np.frombuffer(img_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR) # cv2.IMREAD_COLOR in OpenCV 3.1
    cv2.imshow("Image Stream from " + sid, img)
    cv2.waitKey(1)


if __name__ == '__main__':
    eventlet.wsgi.server(eventlet.listen(('', 9000)), app)