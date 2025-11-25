import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog; 
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Mesclar_RGB implements PlugIn {

    @Override
    public void run(String arg) {

        if (WindowManager.getImageCount() < 3) {
            IJ.error("Erro", "É necessário ter pelo menos 3 imagens abertas.");
            return;
        }

        // Pega os nomes de todas as janelas abertas para colocar na lista de escolha.
        String[] titulos = WindowManager.getImageTitles();

        // Cria uma caixa de diálogo padrão do ImageJ.
        GenericDialog gd = new GenericDialog("Mesclar Canais RGB");

        // O terceiro parâmetro define a seleção padrão (primeira, segunda e terceira janela).
        gd.addChoice("Canal Vermelho (R):", titulos, titulos[0]);
        gd.addChoice("Canal Verde (G):", titulos, titulos[1]);
        gd.addChoice("Canal Azul (B):", titulos, titulos[2]);
        gd.showDialog();

        if (gd.wasCanceled()) return;

        // Recuperamos as imagens baseadas nas escolhas feitas nos menus dropdown.
        ImagePlus imgR = WindowManager.getImage(gd.getNextChoice());
        ImagePlus imgG = WindowManager.getImage(gd.getNextChoice());
        ImagePlus imgB = WindowManager.getImage(gd.getNextChoice());

        if (validarImagens(imgR, imgG, imgB)) {
            construirImagemRGB(imgR, imgG, imgB);
        }
    }

    public boolean validarImagens(ImagePlus r, ImagePlus g, ImagePlus b) {
        // Verifica se todas são 8-bit (escala de cinza/intensidade 0-255)
        if (r.getType() != ImagePlus.GRAY8 || g.getType() != ImagePlus.GRAY8 || b.getType() != ImagePlus.GRAY8) {
            IJ.error("Erro", "Todas as imagens selecionadas devem ser 8-bits (escala de cinza).");
            return false;
        }
        // Verifica se todas têm exatamente a mesma largura e altura
        if (r.getWidth() != g.getWidth() || r.getWidth() != b.getWidth() ||
            r.getHeight() != g.getHeight() || r.getHeight() != b.getHeight()) {
            IJ.error("Erro", "As imagens devem ter as mesmas dimensões.");
            return false;
        }
        return true;
    }

    public void construirImagemRGB(ImagePlus r, ImagePlus g, ImagePlus b) {
        int w = r.getWidth();
        int h = r.getHeight();

        // Pega os processadores para ler os pixels das imagens de entrada
        ImageProcessor ipR = r.getProcessor();
        ImageProcessor ipG = g.getProcessor();
        ImageProcessor ipB = b.getProcessor();

        // Cria uma NOVA imagem vazia, já configurada para ser RGB (colorida)
        ImagePlus imgFinal = IJ.createImage("Resultado RGB", "RGB", w, h, 1);
        ImageProcessor ipFinal = imgFinal.getProcessor();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                // lê a intensidade de cada canal naquela posição
                int valR = ipR.get(x, y);
                int valG = ipG.get(x, y);
                int valB = ipB.get(x, y);

                // empacota os 3 valores em um único inteiro
                // R vai para a esquerda (bits 16-23), G para o meio (bits 8-15), B fica na direita (bits 0-7)
                int pixelRGB = (valR << 16) | (valG << 8) | valB;

                ipFinal.set(x, y, pixelRGB);
            }
        }

        imgFinal.show();
    }
}