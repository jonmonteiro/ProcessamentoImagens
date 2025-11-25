# Processamento de Imagens

## Requisitos
Antes de começar, você precisará ter os seguintes itens instalados:

- [ImageJ](https://imagej.nih.gov/ij/)
- [Apache Ant](https://ant.apache.org/)
- Java Development Kit (JDK)

---

## Instalação

1. **Clone o repositório:**
```bash
git clone https://github.com/Kevin-Perdomo/Processamento_De_Imagens.git
cd Processamento_De_Imagens
Instale o Apache Ant:

bash
Copiar código
sudo apt update
sudo apt install ant
ant -version
Instale o Java 11 (qualquer versão a partir da 8 funciona):

Para instalar o ambiente de execução do OpenJDK:

bash
Copiar código
sudo apt install openjdk-11-jre
Para instalar o ambiente de desenvolvimento do OpenJDK:

bash
Copiar código
sudo apt install openjdk-11-jdk
Verifique a versão instalada:

bash
Copiar código
java --version
Baixe o ImageJ (opcional):

ImageJ

Uso
Importante:
Os nomes dos plugins precisam ter obrigatoriamente um caractere underscore (_) para que o ImageJ os reconheça.
Exemplo: no ImageJ, o plugin Open deve estar associado ao comando Open_.

Compile o projeto completo:

bash
Copiar código
ant run
Compile apenas os plugins:

bash
Copiar código
ant plugins
markdown
Copiar código
