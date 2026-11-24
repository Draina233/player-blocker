import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** Renders the 512x512 project icon: dark rounded tile, red "no" ring, faded player silhouette. */
public final class IconGen {

    public static void main(String[] args) throws Exception {
        int size = 512;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Tile
        RoundRectangle2D tile = new RoundRectangle2D.Double(0, 0, size, size, 104, 104);
        g.setColor(new Color(0x1E2126));
        g.fill(tile);

        // Silhouette, clipped to the ring's inner disc: the figure must sit inside the sign, and
        // the shoulders have to end on a curve or they bleed past the ring at the bottom.
        Shape old = g.getClip();
        g.setClip(new java.awt.geom.Ellipse2D.Double(98, 98, 316, 316));
        g.setColor(new Color(0xE6E9EE));
        g.fillOval(210, 140, 92, 92);        // head
        g.fillOval(166, 254, 180, 206);      // shoulders
        g.setClip(old);

        // Ring
        g.setColor(new Color(0xE03A3A));
        g.setStroke(new BasicStroke(36, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(80, 80, 352, 352);

        // Slash
        g.drawLine(142, 142, 370, 370);

        g.dispose();
        File out = new File(args.length > 0 ? args[0] : "icon.png");
        ImageIO.write(img, "png", out);
        System.out.println("wrote " + out.getAbsolutePath() + " (" + out.length() + " bytes)");
    }
}
