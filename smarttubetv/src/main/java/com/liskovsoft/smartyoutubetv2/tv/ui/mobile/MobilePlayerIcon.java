package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Density-independent player icons, drawn with the same outline weight. */
final class MobilePlayerIcon extends Drawable {
    private final String name;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    MobilePlayerIcon(String name) {
        this.name = name;
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1.8f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    private void line(Canvas c, float... points) {
        Path p = new Path();
        p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) p.lineTo(points[i], points[i + 1]);
        c.drawPath(p, paint);
    }

    @Override public void draw(Canvas c) {
        c.save();
        c.translate(getBounds().left, getBounds().top);
        c.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        paint.setStyle(Paint.Style.STROKE);
        switch (name) {
            case "play":
                paint.setStyle(Paint.Style.FILL);
                line(c, 7, 3, 21, 12, 7, 21, 7, 3);
                break;
            case "pause":
                paint.setStyle(Paint.Style.FILL);
                c.drawRoundRect(5, 3, 10, 21, 1, 1, paint);
                c.drawRoundRect(14, 3, 19, 21, 1, 1, paint);
                break;
            case "down": line(c, 4, 8, 12, 16, 20, 8); break;
            case "chevron-right": line(c, 9, 5, 16, 12, 9, 19); break;
            case "close": line(c, 5, 5, 19, 19); line(c, 19, 5, 5, 19); break;
            case "fullscreen":
                line(c, 3, 9, 3, 3, 9, 3); line(c, 15, 3, 21, 3, 21, 9);
                line(c, 3, 15, 3, 21, 9, 21); line(c, 15, 21, 21, 21, 21, 15);
                break;
            case "fullscreen-exit":
                line(c, 3, 9, 9, 9, 9, 3); line(c, 15, 3, 15, 9, 21, 9);
                line(c, 3, 15, 9, 15, 9, 21); line(c, 15, 21, 15, 15, 21, 15);
                break;
            case "download":
                line(c, 12, 3, 12, 16); line(c, 6, 10, 12, 16, 18, 10);
                line(c, 4, 19, 4, 21, 20, 21, 20, 19); break;
            case "save": line(c, 6, 3, 18, 3, 18, 21, 12, 17, 6, 21, 6, 3); break;
            case "share":
                line(c, 14, 4, 21, 10, 14, 16, 14, 12);
                c.drawPath(sharePath(), paint); break;
            case "search":
                c.drawCircle(10, 10, 7, paint); line(c, 15, 15, 22, 22); break;
            case "settings":
                Path gear = new Path();
                for (int i = 0; i < 32; i++) {
                    double angle = Math.PI * 2 * i / 32;
                    float radius = i % 4 == 0 || i % 4 == 3 ? 10 : 8;
                    float x = 12 + (float) Math.cos(angle) * radius;
                    float y = 12 + (float) Math.sin(angle) * radius;
                    if (i == 0) gear.moveTo(x, y); else gear.lineTo(x, y);
                }
                gear.close(); c.drawPath(gear, paint);
                c.drawCircle(12, 12, 3, paint); break;
            case "like":
                line(c, 7, 10, 11, 3, 14, 3, 14, 9, 21, 9, 19, 21, 7, 21, 7, 10);
                c.drawRect(2, 10, 6, 21, paint); break;
            case "heart": case "heart-filled":
                if (name.equals("heart-filled")) paint.setStyle(Paint.Style.FILL);
                Path heart = new Path();
                heart.moveTo(12, 21);
                heart.cubicTo(9, 18, 2, 13, 2, 8);
                heart.cubicTo(2, 2, 9, 1, 12, 6);
                heart.cubicTo(15, 1, 22, 2, 22, 8);
                heart.cubicTo(22, 13, 15, 18, 12, 21);
                heart.close();
                c.drawPath(heart, paint); break;
            case "dislike":
                c.rotate(180, 12, 12);
                line(c, 7, 10, 11, 3, 14, 3, 14, 9, 21, 9, 19, 21, 7, 21, 7, 10);
                c.drawRect(2, 10, 6, 21, paint); break;
            default:
                paint.setStyle(Paint.Style.FILL);
                c.drawCircle(12, 4, 1.6f, paint); c.drawCircle(12, 12, 1.6f, paint);
                c.drawCircle(12, 20, 1.6f, paint);
        }
        c.restore();
    }

    private Path sharePath() {
        Path p = new Path(); p.moveTo(14, 7); p.cubicTo(5, 7, 3, 12, 3, 19);
        p.cubicTo(7, 13, 10, 12, 14, 12); return p;
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
