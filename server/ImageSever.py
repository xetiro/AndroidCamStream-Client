# This is a simple testing server to receive stream of pictures from
# the AndroidTVCameraToServer app. 
#
# Created by xetiro (aka Ruben Geraldes) on 28/09/2020.
import eventlet
import socketio
import cv2
import numpy as np
from engineio.payload import Payload

# Default is 16 which can create a bootleneck for video streaming
Payload.max_decode_packets = 256

sio = socketio.Server()
app = socketio.WSGIApp(sio)

@sio.event
def connect(sid, environ):
    print('connect', sid)

# This is the main method that the client calls when streaming the pictures to 
# the server. Each receiveImage event is already processed in a new thread.
# The image format is JPEG and is sent by the client in as binary data of byte[] 
# received in python as Bytes.
@sio.event
def receiveImage(sid, imageBytes):
    # Process the image
    print(len(imageBytes))
    show(sid, imageBytes)

@sio.event
def disconnect(sid):
    print('disconnect', sid)
    # Avoids to keep a freezing window in case you used the show method
    cv2.destroyAllWindows

def show(sid, imageBytes):
    nparr = np.frombuffer(imageBytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR) # cv2.IMREAD_COLOR in OpenCV 3.1
    cv2.imshow("Image Stream from " + sid, img)
    cv2.waitKey(1)


if __name__ == '__main__':
    eventlet.wsgi.server(eventlet.listen(('', 9000)), app)