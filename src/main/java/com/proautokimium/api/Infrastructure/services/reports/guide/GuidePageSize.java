package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument;

/**
 * As medidas da página que o layout pede, já com a orientação aplicada.
 *
 * Uma conta só, usada pelo validador e pelo montador: se cada um calculasse a
 * largura útil por conta própria, o dia em que divergissem o validador
 * aprovaria um layout que o Jasper recusa.
 */
public record GuidePageSize(int width, int height, int contentWidth, int contentHeight) {

    public static final String LETTER = "LETTER";
    public static final String A4 = "A4";
    public static final String LANDSCAPE = "LANDSCAPE";
    public static final String PORTRAIT = "PORTRAIT";

    public static GuidePageSize of(GuideLayoutDocument.Page page) {
        int shortSide = A4.equals(page.format()) ? 595 : 612;
        int longSide = A4.equals(page.format()) ? 842 : 792;
        boolean landscape = !PORTRAIT.equals(page.orientation());
        int width = landscape ? longSide : shortSide;
        int height = landscape ? shortSide : longSide;
        GuideLayoutDocument.Margins m = page.margins();
        return new GuidePageSize(width, height,
                width - m.left() - m.right(),
                height - m.top() - m.bottom());
    }
}
