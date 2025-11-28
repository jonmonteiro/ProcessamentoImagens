import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.AWTEvent;

public class Editor_Interativo_Ponto_a_Ponto implements PlugIn, DialogListener {

    private ImagePlus imp; // A imagem ativa (janela)
    private ImageProcessor ipBackup; // Cópia de segurança na memória (não visível)
    
    // Configurações padrão dos sliders
    private static final int BRILHO_PADRAO = 0;
    private static final int CONTRASTE_PADRAO = 0;
    private static final int SOLAR_PADRAO = 256; // 256 desativa (pois pixel max é 255)
    private static final double SAT_PADRAO = 1.0; // 1.0 = 100% cor

    @Override
    public void run(String arg) {
        this.imp = IJ.getImage();

        if (imp.getType() != ImagePlus.COLOR_RGB) {
            IJ.error("Erro", "Este plugin requer uma imagem RGB.");
            return;
        }

        // 1. Snapshot: Guardamos uma cópia limpa da imagem original na memória RAM
        // Usaremos ela como base para calcular os efeitos sem degradar a imagem a cada movimento do mouse.
        this.ipBackup = imp.getProcessor().duplicate();

        exibirInterface();
    }

    private void exibirInterface() {
        GenericDialog gd = new GenericDialog("Ajustes de Imagem");
        gd.addDialogListener(this); // Liga o "escutador" para atualizar em tempo real

        // Sliders
        gd.addSlider("Brilho (Add/Sub)", -100, 100, BRILHO_PADRAO);
        gd.addSlider("Contraste (Fator)", -100, 100, CONTRASTE_PADRAO);
        gd.addSlider("Solarização (Limiar)", 0, 256, SOLAR_PADRAO);
        gd.addSlider("Saturação (0=PB, 1=Normal)", 0, 2.0, SAT_PADRAO);

        gd.showDialog();

        // Lógica do OK / Cancel
        if (gd.wasCanceled()) {
            // Se cancelou, restaura a imagem original usando o backup
            imp.setProcessor(ipBackup);
            imp.updateAndDraw();
        } 
        // Se deu OK, não precisamos fazer nada, pois a imagem na tela 
        // já está modificada pela última chamada do dialogItemChanged.
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        // Se o usuário clicar em Cancelar na interface, paramos de processar
        if (gd.wasCanceled()) return false;

        // Recupera valores dos sliders
        int brilho = (int) gd.getNextNumber();
        int contrasteVal = (int) gd.getNextNumber();
        int limiarSolar = (int) gd.getNextNumber();
        double fatorSat = gd.getNextNumber();

        // Chama o processamento pesado
        aplicarEfeitos(brilho, contrasteVal, limiarSolar, fatorSat);
        
        return true; // Retorna true para manter a interface válida
    }

    /**
     * Aplica todos os efeitos matemáticos pixel a pixel.
     * Utiliza acesso direto ao array (Direct Pixel Access) para máxima velocidade.
     */
    private void aplicarEfeitos(int brilho, int contrasteInput, int limiarSolar, double saturacao) {
        // Pega o processador da imagem visível na tela
        ImageProcessor ipTela = imp.getProcessor();

        // ACESSO DIRETO: Pegamos os pixels como um array gigante de inteiros
        // Isso elimina os loops X e Y e é muito mais rápido que getPixel()
        int[] pixelsTela = (int[]) ipTela.getPixels();
        int[] pixelsOriginais = (int[]) ipBackup.getPixels(); // Lemos sempre do original intacto

        int totalPixels = pixelsTela.length;

        // Pré-cálculo do fator de contraste para não calcular dentro do loop
        // Fórmula clássica de correção de contraste
        double fatorContraste = (1.0 + (contrasteInput / 100.0));
        fatorContraste = fatorContraste * fatorContraste; // Curva quadrática para suavizar

        // Loop Unificado (percorre todos os pixels de uma vez)
        for (int i = 0; i < totalPixels; i++) {
            
            // 1. Extração rápida de cores (Bitwise) da imagem ORIGINAL
            int c = pixelsOriginais[i];
            int r = (c >> 16) & 0xff;
            int g = (c >> 8) & 0xff;
            int b = c & 0xff;

            // 2. Aplicação de Brilho
            r += brilho;
            g += brilho;
            b += brilho;

            // 3. Aplicação de Contraste
            // (Cor - 128) * fator + 128 -> Expande a distância do cinza médio
            r = (int)((r - 128) * fatorContraste + 128);
            g = (int)((g - 128) * fatorContraste + 128);
            b = (int)((b - 128) * fatorContraste + 128);

            // 4. Aplicação de Solarização (Inversão condicional)
            if (r > limiarSolar) r = 255 - r;
            if (g > limiarSolar) g = 255 - g;
            if (b > limiarSolar) b = 255 - b;

            // 5. Clamping (Grampear valores entre 0 e 255 ANTES da saturação)
            // É vital garantir que não estourou 255 ou ficou negativo
            r = (r > 255) ? 255 : ((r < 0) ? 0 : r);
            g = (g > 255) ? 255 : ((g < 0) ? 0 : g);
            b = (b > 255) ? 255 : ((b < 0) ? 0 : b);

            // 6. Saturação (Mistura com a média cinza)
            if (saturacao != 1.0) {
                // Média ponderada (Luminância) é mais precisa que média simples
                int lum = (int)(r * 0.299 + g * 0.587 + b * 0.114);
                
                // Fórmula de Interpolação Linear (Lerp)
                // NovoValor = Cinza + (Cor - Cinza) * Fator
                r = (int)(lum + (r - lum) * saturacao);
                g = (int)(lum + (g - lum) * saturacao);
                b = (int)(lum + (b - lum) * saturacao);
                
                // Clamp novamente após saturação
                r = (r > 255) ? 255 : ((r < 0) ? 0 : r);
                g = (g > 255) ? 255 : ((g < 0) ? 0 : g);
                b = (b > 255) ? 255 : ((b < 0) ? 0 : b);
            }

            // 7. Remontagem do pixel e gravação na TELA
            pixelsTela[i] = (r << 16) | (g << 8) | b;
        }

        // Avisa o ImageJ que mudamos os pixels para ele repintar a janela
        imp.updateAndDraw();
    }
}