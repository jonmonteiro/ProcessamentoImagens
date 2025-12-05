import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Histograma_Expansao_Equalizacao implements PlugIn {

    @Override
    public void run(String arg) {
        ImagePlus imagemOriginal = IJ.getImage();

        // Validação: Garante que é uma imagem 8-bit (tons de cinza)
        if (imagemOriginal.getType() != ImagePlus.GRAY8) {
            IJ.error("Erro", "A imagem precisa estar em escala de cinza (8-bit).");
            return;
        }

        // Exibe o histograma de como a imagem é AGORA
        mostrarGraficoHistograma(imagemOriginal, "Histograma Original");

        // Abre a caixa de diálogo
        GenericDialog gd = new GenericDialog("Processamento de Histograma");
        String[] opcoes = {"Expansão", "Equalização"};
        gd.addRadioButtonGroup("Método:", opcoes, 2, 1, opcoes[0]);
        gd.showDialog();

        if (gd.wasCanceled()) return;

        // Processamento
        String metodo = gd.getNextRadioButton();
        
        // Cria uma cópia para não estragar a original se der erro
        ImagePlus imagemResultado = imagemOriginal.duplicate();
        imagemResultado.setTitle("Resultado - " + metodo);
        
        if (metodo.equals(opcoes[0])) {
            aplicarExpansao(imagemResultado);
        } else {
            aplicarEqualizacao(imagemResultado);
        }

        // Mostra a imagem nova e o histograma novo
        imagemResultado.show();
        mostrarGraficoHistograma(imagemResultado, "Histograma após " + metodo);
    }

    // --- MÉTODOS DE PROCESSAMENTO ---

    private void aplicarExpansao(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        int[] histograma = ip.getHistogram(); // O ImageJ já calcula isso rápido pra nós!
        
        // 1. Encontrar o tom Mínimo (primeiro índice com valor > 0)
        int tomMin = 0;
        for (int i = 0; i < 256; i++) {
            if (histograma[i] > 0) {
                tomMin = i;
                break;
            }
        }

        // 2. Encontrar o tom Máximo (último índice com valor > 0)
        int tomMax = 255;
        for (int i = 255; i >= 0; i--) {
            if (histograma[i] > 0) {
                tomMax = i;
                break;
            }
        }

        // 3. Criar a Tabela de Consulta (LUT)
        // Isso evita fazer conta de divisão dentro do loop de pixels
        int[] tabelaLUT = new int[256];
        double range = tomMax - tomMin;
        
        for (int i = 0; i < 256; i++) {
            if (i < tomMin) {
                tabelaLUT[i] = 0;
            } else if (i > tomMax) {
                tabelaLUT[i] = 255;
            } else {
                // Fórmula: (Valor - Min) * (255 / Range)
                tabelaLUT[i] = (int) (( (i - tomMin) / range ) * 255.0);
            }
        }

        // 4. Aplicar a tabela na imagem
        aplicarLutNaImagem(ip, tabelaLUT);
    }

    private void aplicarEqualizacao(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        int[] histograma = ip.getHistogram();
        int totalPixels = ip.getPixelCount();

        // 1. Calcular Histograma Acumulado (CDF) e LUT simultaneamente
        int[] tabelaLUT = new int[256];
        long somaAcumulada = 0; // 'long' para evitar estouro em imagens gigantes

        // Encontra o primeiro valor não nulo (cdfMin) para normalização perfeita (opcional, mas recomendado)
        // Aqui usaremos a fórmula simplificada: (cdf * 255) / total
        
        for (int i = 0; i < 256; i++) {
            somaAcumulada += histograma[i];
            
            // Fórmula da Equalização: CDF * (L - 1)
            // Math.round para arredondar corretamente o tom de cinza
            tabelaLUT[i] = (int) Math.round((double) somaAcumulada * 255 / totalPixels);
        }

        // 2. Aplicar a tabela na imagem
        aplicarLutNaImagem(ip, tabelaLUT);
    }

    // --- MÉTODOS AUXILIARES ---

    /**
     * Aplica uma tabela de transformação (LUT) usando vetor de bytes.
     * Substitui getPixel/putPixel por acesso direto à memória.
     */
    private void aplicarLutNaImagem(ImageProcessor ip, int[] tabelaLUT) {
        // Acesso direto ao vetor de bytes (A tal "Tripa" de pixels)
        // Nota: Em Java, bytes vão de -128 a 127. Precisamos tratar isso.
        byte[] pixels = (byte[]) ip.getPixels();

        for (int i = 0; i < pixels.length; i++) {
            // Conversão de byte assinado para inteiro 0-255
            // (0xff é a máscara que corrige o sinal negativo do Java)
            int valorOriginal = pixels[i] & 0xff;

            // Consulta a tabela: "Se o valor era X, qual é o novo valor?"
            int novoValor = tabelaLUT[valorOriginal];

            // Grava de volta no vetor (fazendo cast para byte)
            pixels[i] = (byte) novoValor;
        }
    }

    private void mostrarGraficoHistograma(ImagePlus imp, String titulo) {
        int[] hist = imp.getProcessor().getHistogram();
        
        // Prepara dados para o Plot (X = tons, Y = frequência)
        double[] x = new double[256];
        double[] y = new double[256];
        
        for (int i = 0; i < 256; i++) {
            x[i] = i;
            y[i] = hist[i];
        }

        Plot plot = new Plot(titulo, "Tom de Cinza (0-255)", "Quantidade de Pixels");
        plot.add("bar", x, y); // Tipo 'bar' desenha como gráfico de barras
        plot.show();
    }
}