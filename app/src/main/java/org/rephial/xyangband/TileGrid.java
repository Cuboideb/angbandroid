package org.rephial.xyangband;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Bitmap;

class TileGrid
{
    int attr;
    int chr;
    Point srcPoint;
    TSize srcSize;

    Point dstPoint;
    TSize dstSize;

    Point ptInPage;
    Point page;

    GraphicsMode gm;
    TermView term;

    boolean isLargeTile = false;

    String key = "";

    public TileGrid(GraphicsMode pGM, int pSrcRow, int pSrcCol,
        int pDstRow, int pDstCol, TermView pTerm)
    {
        gm = pGM;
        term = pTerm;

        attr = pSrcRow;
        chr = pSrcCol;

        if (term.useGraphics == Preferences.MICROCHASM_GX) {
            srcPoint = new Point(chr & 0x3F, attr & 0x3F);
        }
        else {
            srcPoint = new Point(chr & 0x7F, attr & 0x7F);
        }

        srcSize = new TSize(gm.cell_width, gm.cell_height);

        dstPoint = new Point(pDstCol, pDstRow);

        dstSize = new TSize(term.tile_wid * term.char_width,
            term.tile_hgt * term.char_height);

        isLargeTile = false;

        if (gm.isLargeTile(srcPoint.y, srcPoint.x) && dstPoint.y > 2) {

            isLargeTile = true;

            srcPoint.y -= 1;
            srcSize.height *= 2;

            dstPoint.y -= term.tile_hgt;
            dstSize.height *= 2;
        }

        page = new Point(0, 0);
        ptInPage = new Point(srcPoint);

        if (gm.havePages()) {
            ptInPage.x %= gm.pageSize;
            ptInPage.y %= gm.pageSize;

            page.x = srcPoint.x / gm.pageSize;
            page.y = srcPoint.y / gm.pageSize;
        }
    }

    public void changeSource(int a, int c)
    {
        attr = a;
        srcPoint.y = ptInPage.y = (a & 0x7F);

        chr = c;
        srcPoint.x = ptInPage.x = (c & 0x7F);

        key = "";
    }

    public Rect locateSource()
    {
        int x = ptInPage.x * gm.cell_width;
        int y = ptInPage.y * gm.cell_height;
        return new Rect(x, y, x + srcSize.width,
            y + srcSize.height);
    }

    public Rect locateDest()
    {
        int x = dstPoint.x * term.char_width;
        int y = dstPoint.y * term.char_height;
        return new Rect(x, y, x + dstSize.width,
            y + dstSize.height);
    }

    public String createKey()
    {
        if (key == "") {
            key = "" + gm.idx + "-" + srcPoint.x + "@" + srcPoint.y +
                "-" + dstSize.width + "x" + dstSize.height;
        }
        return key;
    }

    public Bitmap loadBitmap(Bitmap from)
    {
        Rect src = locateSource();

        Bitmap result = null;

        if (src.left < 0 || src.top < 0) return null;

        int sw = src.right - src.left;
        int sh = src.bottom - src.top;

        if (sw <= 0 || src.left + sw > from.getWidth()) return null;
        if (sh <= 0 || src.top + sh > from.getHeight()) return null;

        if (dstSize.width <= 0 || dstSize.height <= 0) return null;

        try {
            Bitmap region = Bitmap.createBitmap(from, src.left,
                    src.top, sw, sh);

            result = Bitmap.createScaledBitmap(region,
                    dstSize.width, dstSize.height, true);

            region.recycle();
        }
        catch (java.lang.IllegalArgumentException ex) {
            result = null;
        }

        return result;
    }
}
