import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.AWTEvent;

public class Editor_Interativo_Ponto_a_Ponto implements PlugIn, DialogListener {

    private ImagePlus imagePlus;
    private ImageProcessor imageBackup;
    
    private static final int BRILHO_PADRAO = 0;
    private static final int CONTRASTE_PADRAO = 0;
    private static final int SOLAR_PADRAO = 256;
    private static final double SAT_PADRAO = 1.0; 

    @Override
    public void run(String arg) {
        
        this.imagePlus = IJ.getImage();

        if (imagePlus.getType() != ImagePlus.COLOR_RGB) {
            IJ.error("Erro", "Selecione uma imagem RGB.");
            return;
        }

        //copia limpa da imagem original
        this.imageBackup = imagePlus.getProcessor().duplicate();

        GenericDialog gd = new GenericDialog("Ajustes de Imagem");
        gd.addDialogListener(this); //listener para atualizar em tempo real

        gd.addSlider("Brilho", -100, 100, BRILHO_PADRAO);
        gd.addSlider("Contraste", -255, 255, CONTRASTE_PADRAO); 
        gd.addSlider("Solarização", 0, 256, SOLAR_PADRAO);
        gd.addSlider("Dessaturação", 0.0, 1.0, SAT_PADRAO);

        gd.showDialog();

        if (gd.wasCanceled()) {
            imagePlus.setProcessor(imageBackup);
            imagePlus.updateAndDraw();
        } 
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        if (gd.wasCanceled()) return false;

        int brilho = (int) gd.getNextNumber();
        int contraste = (int) gd.getNextNumber();
        int solarizacao = (int) gd.getNextNumber();
        double saturacao = gd.getNextNumber();

        aplicarEfeitos(brilho, contraste, solarizacao, saturacao);
        
        return true;
    }

    private void aplicarEfeitos(int brilho, int contraste, int solarizacao, double saturacao) {
        ImageProcessor imageProcessorAtual = imagePlus.getProcessor();

        int[] pixelsImagem = (int[]) imageProcessorAtual.getPixels();
        int[] pixelsOriginais = (int[]) imageBackup.getPixels();

        int totalPixels = pixelsImagem.length;

        float fatorContraste = (259f * (contraste + 255f)) / (255f * (259f - contraste));

        for (int i = 0; i < totalPixels; i++) {
            
            //extração de cores da imagem
            int pixelAtual = pixelsOriginais[i];
            int r = (pixelAtual >> 16) & 0xff;
            int g = (pixelAtual >> 8) & 0xff;
            int b = pixelAtual & 0xff;

            r += brilho;
            g += brilho;
            b += brilho;

            //controle de intensidade
            r = (r > 255) ? 255 : ((r < 0) ? 0 : r);
            g = (g > 255) ? 255 : ((g < 0) ? 0 : g);
            b = (b > 255) ? 255 : ((b < 0) ? 0 : b);

            r = (int)((r - 128) * fatorContraste + 128);
            g = (int)((g - 128) * fatorContraste + 128);
            b = (int)((b - 128) * fatorContraste + 128);

            r = (r > 255) ? 255 : ((r < 0) ? 0 : r);
            g = (g > 255) ? 255 : ((g < 0) ? 0 : g);
            b = (b > 255) ? 255 : ((b < 0) ? 0 : b);

            if (r > solarizacao) r = 255 - r;
            if (g > solarizacao) g = 255 - g;
            if (b > solarizacao) b = 255 - b;

            if (saturacao != 1.0) {
                int lum = (int)(r * 0.299 + g * 0.587 + b * 0.114);
                
                r = (int)(lum + (r - lum) * saturacao);
                g = (int)(lum + (g - lum) * saturacao);
                b = (int)(lum + (b - lum) * saturacao);
                
                r = (r > 255) ? 255 : ((r < 0) ? 0 : r);
                g = (g > 255) ? 255 : ((g < 0) ? 0 : g);
                b = (b > 255) ? 255 : ((b < 0) ? 0 : b);
            }

            pixelsImagem[i] = (r << 16) | (g << 8) | b;
        }

        imagePlus.updateAndDraw();
    }
}