import cv2
import numpy as np

#read image
src = cv2.imread('C:/Images/monaLisa.jpg', cv2.IMREAD_UNCHANGED)

# assign red channel to zeros
src[:,:,2] = np.zeros([src.shape[0], src.shape[1]])

#save image
cv2.imwrite('C:/Images/filterRemoveRedChannelImage.jpg',src)