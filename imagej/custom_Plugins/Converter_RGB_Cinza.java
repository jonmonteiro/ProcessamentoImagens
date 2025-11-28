import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.Checkbox;

public class Converter_RGB_Cinza implements PlugIn {

    // Constantes para identificar os métodos (facilita leitura)
    private static final String MEDIA = "Média Aritmética";
    private static final String LUM_ANALOG = "Luminância Analógica";
    private static final String LUM_DIGITAL = "Luminância Digital";

    @Override
    public void run(String arg) {
        // 1. Verificação de segurança
        ImagePlus imgAtual = IJ.getImage();
        if (imgAtual.getType() != ImagePlus.COLOR_RGB) {
            IJ.error("Erro", "Por favor, selecione uma imagem RGB.");
            return;
        }

        // 2. Apresenta o Diálogo
        GenericDialog gd = new GenericDialog("Conversão");
        
        String[] metodos = {MEDIA, LUM_ANALOG, LUM_DIGITAL};
        
        gd.addRadioButtonGroup("Estratégia de Conversão:", metodos, 3, 1, MEDIA);
        gd.addCheckbox("Criar nova imagem", true);
        
        gd.showDialog();

        if (gd.wasCanceled()) return;

        // 3. Captura opções do usuário
        String metodoEscolhido = gd.getNextRadioButton();
        boolean criarNova = gd.getNextBoolean();

        // 4. Executa o processamento
        aplicarConversao(imgAtual, metodoEscolhido, criarNova);
    }

    private void aplicarConversao(ImagePlus imp, String metodo, boolean criarNova) {
        int largura = imp.getWidth();
        int altura = imp.getHeight();
        ImageProcessor ipOrigem = imp.getProcessor(); // Leitura

        // Define onde vamos escrever o resultado
        ImagePlus impDestino;
        ImageProcessor ipDestino;

        if (criarNova) {
            // Cria uma nova imagem 8-bit (escala de cinza real)
            impDestino = IJ.createImage("Cinza - " + metodo, "8-bit", largura, altura, 1);
            ipDestino = impDestino.getProcessor();
        } else {
            // Modifica a própria imagem (mantém formato RGB, mas visualmente cinza)
            impDestino = imp; // Aponta para a original
            ipDestino = imp.getProcessor(); // Escreve no processador original
        }

        // -- OTIMIZAÇÃO: Definir os pesos ANTES do loop --
        // Isso evita fazer "if/switch" milhões de vezes dentro do loop.
        double pesoR = 0, pesoG = 0, pesoB = 0; // Pesos (Ponderação)
        boolean media = false;

        switch (metodo) {
            case MEDIA:
                media = true; // Tratamento especial para divisão inteira
                break;
            case LUM_ANALOG: 
                // Padrão NTSC / CCIR 601 (TV Antiga/JPEG)
                pesoR = 0.299; pesoG = 0.587; pesoB = 0.114;
                break;
            case LUM_DIGITAL:
                // Padrão HDTV / ITU-R Rec. 709 (Monitores modernos)
                pesoR = 0.2126; pesoG = 0.7152; pesoB = 0.0722;
                break;
        }

        // -- LOOP DE PROCESSAMENTO --
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                
                // 1. Extração rápida com Bitwise (sem arrays)
                int pixel = ipOrigem.get(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;

                // 2. Cálculo do valor cinza
                int cinza;
                if (media) {
                    cinza = (r + g + b) / 3;
                } else {
                    // Fórmula: Soma ponderada pelos coeficientes
                    cinza = (int) (r * pesoR + g * pesoG + b * pesoB);
                }

                // 3. Escrita do resultado
                if (criarNova) {
                    // Imagem 8-bit só precisa de 1 valor (0-255)
                    ipDestino.set(x, y, cinza);
                } else {
                    // Imagem RGB precisa empacotar o cinza nos 3 canais
                    // (cinza << 16) | (cinza << 8) | cinza
                    int cinzaRGB = (cinza << 16) | (cinza << 8) | cinza;
                    ipDestino.set(x, y, cinzaRGB);
                }
            }
        }

        // Atualiza e mostra
        impDestino.show();
        if (!criarNova) {
            imp.updateAndDraw(); // Força a atualização da tela se alterou a original
        }
    }
}