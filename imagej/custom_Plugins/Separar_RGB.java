import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Separar_RGB implements PlugIn {

    @Override
    public void run(String arg) {

        if (WindowManager.getImageCount() == 0) {
            IJ.error("Erro", "Nenhuma imagem aberta. Abra uma imagem RGB.");
            return;
        }

        ImagePlus imgOriginal = IJ.getImage();

        if (imgOriginal.getType() != ImagePlus.COLOR_RGB) {
            IJ.error("Erro", "Abra uma imagem RGB.");
            return;
        }

        extrairCanais(imgOriginal);
    }

    public void extrairCanais(ImagePlus implus) {
    
    int largura = implus.getWidth();
    int altura = implus.getHeight();
    
    // objeto que manipula os pixels.
    ImageProcessor ipOriginal = implus.getProcessor();
   
    ImagePlus impR = IJ.createImage(implus.getTitle() + " (Red)", "8-bit", largura, altura, 1);
    ImagePlus impG = IJ.createImage(implus.getTitle() + " (Green)", "8-bit", largura, altura, 1);
    ImagePlus impB = IJ.createImage(implus.getTitle() + " (Blue)", "8-bit", largura, altura, 1);

    // processors das novas imagens para manipular
    ImageProcessor ipR = impR.getProcessor();
    ImageProcessor ipG = impG.getProcessor();
    ImageProcessor ipB = impB.getProcessor();

    for (int x = 0; x < largura; x++) {
        for (int y = 0; y < altura; y++) {
            
            // pega o pixel da posição da img original (x,y) 
            int pixel = ipOriginal.getPixel(x, y);
            
            // extrai bits referentes ao vermelho que começa no bit 16
            // antes: bits AAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB
            // depois de aplicar Bitwise >> 16: 0000000000000000AAAAAAAARRRRRRRR
            // 16-23
            int r = (pixel >> 16) & 0xff; 
            
            // 8-15
            int g = (pixel >> 8) & 0xff;
            
            // 0-7
            int b = pixel & 0xff;

            // na posição x y pinta o pixel com a intensidade extraida
            ipR.set(x, y, r);
            ipG.set(x, y, g);
            ipB.set(x, y, b);
        }
    }

    impR.show();
    impG.show();
    impB.show();
    }
}