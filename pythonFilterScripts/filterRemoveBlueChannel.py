import cv2
import numpy as np

#read image
src = cv2.imread('C:/Images/monaLisa.jpg', cv2.IMREAD_UNCHANGED)

# assign blue channel to zeros
src[:,:,0] = np.zeros([src.shape[0], src.shape[1]])

#save image
cv2.imwrite('C:/Images/filterRemoveBlueChannelImage.jpg',src)