package org.rephial.xyangband;

import android.graphics.Point;

public class GraphicsMode {
    int idx;
    String name;
    String directory;
    String file;        
    int cell_width;
    int cell_height;
    int alphaBlend;
    int overdrawRow;
    int overdrawMax;
    int pageSize;        

    public GraphicsMode() {            
    }

    public boolean isLargeTile(int row, int col) {

        if (overdrawRow == 0 || overdrawMax == 0) return false;

        if (row >= overdrawRow && row <= overdrawMax) return true;

        // Some large tiles are placed outside the overdraw rows
        if (row == 27 && col >= 122) return true;
        
        return false;            
    }

    public boolean havePages() {

        return pageSize > 0;
    }

    public String getFullPath(Point page)
    {
        String pathToPicture = Preferences.getAngbandFilesDirectory() +
            "/tiles/" + directory + "/" + file;

        if (page != null) {
            pathToPicture = pathToPicture.replace(".png",
                "-" + page.x + "-" + page.y + ".png");
        }

        return pathToPicture;
    }
};