import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.Checkbox;

public class Converter_RGB_Cinza implements PlugIn {

    private static final String MEDIA = "Média Aritmética";
    private static final String LUM_ANALOG = "Luminância Analógica";
    private static final String LUM_DIGITAL = "Luminância Digital";

    @Override
    public void run(String arg) {

        ImagePlus imgAtual = IJ.getImage();
        if (imgAtual.getType() != ImagePlus.COLOR_RGB) {
            IJ.error("Erro", "Por favor, selecione uma imagem RGB.");
            return;
        }

        GenericDialog gd = new GenericDialog("Conversão");
        
        String[] metodos = {MEDIA, LUM_ANALOG, LUM_DIGITAL};
        
        gd.addRadioButtonGroup("Estratégia de Conversão:", metodos, 3, 1, MEDIA);
        gd.addCheckbox("Criar nova imagem", true);
        
        gd.showDialog();

        if (gd.wasCanceled()) return;

        String metodoEscolhido = gd.getNextRadioButton();
        boolean criarNova = gd.getNextBoolean();

        aplicarConversao(imgAtual, metodoEscolhido, criarNova);
    }

    private void aplicarConversao(ImagePlus imp, String metodo, boolean criarNova) {

        int largura = imp.getWidth();
        int altura = imp.getHeight();
        ImageProcessor imageProcessorOrigem = imp.getProcessor(); 
        ImagePlus imagePlusDestino;
        ImageProcessor imageProcessorDestino;

        if (criarNova) {
            imagePlusDestino = IJ.createImage("Cinza - " + metodo, "8-bit", largura, altura, 1);
            imageProcessorDestino = imagePlusDestino.getProcessor();
        } else {
            imagePlusDestino = imp;
            imageProcessorDestino = imp.getProcessor();
        }

        double pesoR = 0, pesoG = 0, pesoB = 0;
        boolean media = false;

        switch (metodo) {
            case MEDIA:
                media = true; 
                break;
            case LUM_ANALOG: 
                pesoR = 0.299; pesoG = 0.587; pesoB = 0.114;
                break;
            case LUM_DIGITAL:
                pesoR = 0.2126; pesoG = 0.7152; pesoB = 0.0722;
                break;
        }

        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                
                //leitura dos pixels rgb
                int pixel = imageProcessorOrigem.get(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;

                int cinza;
                if (media) {
                    cinza = (r + g + b) / 3;
                } else {
                    cinza = (int) (r * pesoR + g * pesoG + b * pesoB);
                }

                if (criarNova) {
                    //imagem 8 bits
                    imageProcessorDestino.set(x, y, cinza);
                } else {
                    //imagem rgb, agrupa os valores cinza nos 3 canais
                    int cinzaRGB = (cinza << 16) | (cinza << 8) | cinza;
                    imageProcessorDestino.set(x, y, cinzaRGB);
                }
            }
        }

        imagePlusDestino.show();
        if (!criarNova) {
            imp.updateAndDraw(); //atualização da tela se alterou a original
        }
    }
}