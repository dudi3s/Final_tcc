package tcc.tcc_final;

import auxiliares.Frequencia;
import auxiliares.Lexico;
import auxiliares.Tweet;
import auxiliares.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static java.util.Collections.list;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.cogroo.analyzer.Analyzer;
import org.cogroo.analyzer.ComponentFactory;
import org.cogroo.checker.CheckDocument;
import org.cogroo.checker.GrammarChecker;
import org.cogroo.text.Document;
import org.cogroo.text.impl.DocumentImpl;

public class MainApp extends Application {

    private static boolean ASC = true;
    private static boolean DESC = false;
    private static String mes = "setembro";
    private static String a1 = "anotacao/anotados/csv/tweets_Joao_c.csv";
    private static String a2 = "anotacao/anotados/csv/tweets_Natassia_c.csv";
    private static String a3 = "anotacao/anotados/csv/tweets_Robinson_c.csv";

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        stage.setTitle("JavaFX and Maven");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws IOException {
        //launch(args);

        //Dicionário de palavras abreviadas e escritas pela internet, utilizado para "normalização" do texto;
        HashMap<String, String> dicionario_internetes = buildDicionarioInternet("dicionarios/dicionario_internetes.txt");

        //Dicionário Léxico contendo a polaridade de diversas palavras para o cálculo da polarização do tweet;
        HashMap<String, Lexico> dicionario_lexico = buildDicionarioLexico("dicionarios/lexicos/");

        //Dataset original de tweets coletados;
        HashMap<String, Tweet> datasetOriginal = buildDataSet("coletas/exp_all/", dicionario_internetes);

        //Lista com os ids dos tweets anotados e suas respectivas polaridades;
        HashMap<String, Integer> t_pol = ids_anotadosManual(a1, a2, a3);

        buildDataSetAnotado(t_pol, dicionario_lexico, datasetOriginal);
        System.out.println("DONE!");

    }

    //Método que lê o arquivo .JSON e realiza a construção do dataset em memória do mesmo
    //utilizando a API Jackson
    private static HashMap<String, Tweet> buildDataSet(String path_mes_dir, HashMap<String, String> dic_internetes) throws IOException {
        HashMap<String, Tweet> tweets = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        File folder = new File(path_mes_dir);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {

                try {
                    FileReader fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr);

                    String line = br.readLine();
                    while (line != null) {

                        JsonNode actualObj = mapper.readTree(line);

                        //Informações do Tweet
                        String id = actualObj.get("id").asText();
                        String lang = actualObj.get("lang").asText();
                        String created = actualObj.get("created_at").asText();
                        String text = actualObj.get("text").asText();
                        int retweeted = actualObj.get("retweet_count").asInt();

                        //Informações do Usuário que postou o tweet
                        String user_id = actualObj.get("user").get("id_str").asText();
                        int followers = actualObj.get("user").get("followers_count").asInt();
                        String location = actualObj.get("user").get("location").asText();
                        String user_name = actualObj.get("user").get("screen_name").asText();

                        //Pegar somente a parte do texto, caso contenha um retweet, digitada
                        //pelo usuário, ignorando o texto do retweet.
                        if (text.contains("RT")) {
                            text = text.substring(0, text.indexOf("RT"));
                        }

                        String rtText = "";
                        if (actualObj.has("rt_text")) {
                            rtText = actualObj.get("rt_text").asText();
                        }

                        //Se há algo que a pessoa digitou ou acrescentou algo a algum retweet
                        //então essa opinião que é salva.
                        if (!text.equals("") || (!text.equals("") && !rtText.equals(""))) {
                            //Remoção de quebras de linhas e ENTERs.
                            text = StringUtils.normalizeSpace(text).toLowerCase();
                            text = removeUrl(text);
                            text = text.replace("#", "");
                            text = text.replace("...", "");
                            text = text.replace(";", "");

                            String[] temp = text.toLowerCase().split(" ");
                            StringBuilder textoTratado = new StringBuilder(text.length());

                            for (String s : temp) {
                                if (dic_internetes.containsKey(s)) {
                                    textoTratado.append(dic_internetes.get(s));
                                } else {
                                    textoTratado.append(s);
                                }

                                textoTratado.append(" ");
                            }

                            text = StringUtils.normalizeSpace(textoTratado.toString());
                            boolean atleastOneAlpha = text.matches(".*[a-zA-Z]+.*");

                            if (!text.equals("") && atleastOneAlpha) {
                                User u = new User(user_id, location, user_name, followers);
                                Tweet t = new Tweet(id, lang, text, created, retweeted, u);

                                tweets.put(id, t);
                            }
                        }

                        line = br.readLine();
                    }

                    br.close();
                    fr.close();

                } catch (FileNotFoundException ex) {
                    System.out.println("Caminho não localizado: " + path_mes_dir);
                } catch (IOException ex) {
                    System.out.println("Não foi possível ler o arquivo");
                }
            }
        }

        return tweets;
    }

    private static HashMap<String, Tweet> buildDataSetAnotado(HashMap<String, Integer> tweets_anotados, HashMap<String, Lexico> dicionario_lexico, HashMap<String, Tweet> datasetOriginal) throws FileNotFoundException, IOException {
        HashMap<String, Tweet> datasetAnotado = new HashMap<>();

        List<String> verbosLig = new ArrayList<>();
        verbosLig.add("ser");
        verbosLig.add("estar");
        verbosLig.add("parecer");
        verbosLig.add("permanecer");
        verbosLig.add("ficar");
        verbosLig.add("continuar");
        verbosLig.add("andar");
        verbosLig.add("tornar");
        verbosLig.add("ano");
        verbosLig.add("não");
        verbosLig.add("ter");
        verbosLig.add("mais");
        verbosLig.add("ir");
        verbosLig.add("já");
        verbosLig.add("como");
        verbosLig.add("fazer");
        verbosLig.add("haver");
        verbosLig.add("poder");
        verbosLig.add("muito");
        verbosLig.add("só");

        int tweets_read = 1;
        ComponentFactory factory = ComponentFactory.create(new Locale("pt", "BR"));
        Analyzer cogroo = factory.createPipe();

        Document document = new DocumentImpl();

        for (Entry<String, Integer> t : tweets_anotados.entrySet()) {
            Tweet fromDB = datasetOriginal.get(t.getKey());

            String text_final = fromDB.getText();
            HashMap<Lexico, Frequencia> freq_uni = fromDB.getFreq_unigrama();
            HashMap<Lexico, Frequencia> freq_bi = fromDB.getFreq_bigrama();

            document.setText(text_final);
            cogroo.analyze(document);

            document.getSentences().forEach((sentence) -> {
                sentence.getTokens().forEach((token) -> {
                    for (String s : token.getLemmas()) {
                        String t_type = token.getPOSTag();
                        s = s.toLowerCase();

                        if ((t_type.equals("n") || t_type.equals("prop") || t_type.equals("adj") || t_type.equals("adv")
                                || t_type.equals("v-fin") || t_type.equals("v-inf") || t_type.equals("v-pcp") || t_type.equals("v-ger")) && !verbosLig.contains(s)) {

                            int polaridade = 1;
                            char anot = 'M';

                            Lexico aux = dicionario_lexico.get(s);
                            //Se o token atual possui sentimento de acordo com o dic léxico;
                            if (aux != null) {
                                anot = aux.getTipo_anotacao();

                                switch (aux.getPolaridade()) {
                                    case -1:
                                        polaridade = -2;
                                        break;
                                    case 1:
                                        polaridade = 2;
                                        break;
                                    default:
                                        polaridade = 1;
                                }
                            }

                            //Lexico que representa um token anotado manualmente a polaridade;
                            Lexico l = new Lexico(s, t_type, anot, polaridade);

                            //Adicionar o léxico à lista do tweet respectivo contabilizando sua frequencia;
                            if (freq_uni.containsKey(l)) {
                                Frequencia f = freq_uni.get(l);
                                int fA = f.getTf();
                                f.setTf(fA + 1);
                                freq_uni.put(l, f);
                            } else {
                                freq_uni.put(l, new Frequencia(1, 0.0));
                            }
                        }
                    }
                });

                sentence.getSyntacticChunks().forEach((structure) -> {
                    List<Lexico> aux = new ArrayList<>();

                    structure.getTokens().forEach((token) -> {
                        for (String s : token.getLemmas()) {
                            String t_type = token.getPOSTag();
                            s = s.toLowerCase();

                            if ((t_type.equals("n") || t_type.equals("prop") || t_type.equals("adj") || t_type.equals("adv")
                                    || t_type.equals("v-fin") || t_type.equals("v-inf") || t_type.equals("v-pcp") || t_type.equals("v-ger")) && !verbosLig.contains(s)) {

                                int polaridade = 1;
                                char anot = 'M';

                                Lexico l_aux = dicionario_lexico.get(s);
                                //Se o token atual possui sentimento de acordo com o dic léxico;
                                if (l_aux != null) {
                                    anot = l_aux.getTipo_anotacao();

                                    switch (l_aux.getPolaridade()) {
                                        case -1:
                                            polaridade = -2;
                                            break;
                                        case 1:
                                            polaridade = 2;
                                            break;
                                        default:
                                            polaridade = 1;
                                    }
                                }

                                //Lexico que representa um token anotado manualmente a polaridade;
                                Lexico l = new Lexico(s, t_type, anot, polaridade);
                                aux.add(l);
                            }
                        }
                    });

                    if (aux.size() > 1) {
                        for (int i = 0; i < aux.size() - 1; i = i + 2) {
                            StringBuilder bigr = new StringBuilder();

                            //Realizar o somatório das polaridades para definir a do termo composto.
                            Lexico first = aux.get(i);
                            Lexico second = aux.get(i + 1);

                            bigr.append(first.getPalavra()).append(" ").append(second.getPalavra());

                            int f_pol = first.getPolaridade();
                            int s_pol = second.getPolaridade();

                            int pol_final = 1;

                            //Caso os dois termos não sejam neutros;
                            if (f_pol != 1 && s_pol != 1) {
                                int result = f_pol + s_pol;
                                if (result > 0) {
                                    pol_final = 2;
                                } else {
                                    pol_final = -2;
                                }
                            }

                            //Realizar a contagem de frequencia;
                            Lexico fin = new Lexico(bigr.toString(), first.getPostag() + "+" + second.getPostag(), 'M', pol_final);

                            if (freq_bi.containsKey(fin)) {
                                Frequencia f = freq_bi.get(fin);
                                int fA = f.getTf();
                                f.setTf(fA + 1);
                                freq_bi.put(fin, f);

                            } else {
                                freq_bi.put(fin, new Frequencia(1, 0.0));
                            }
                        }
                    }
                });

            });

            int totalUni = freq_uni.size();
            int totalBi = freq_bi.size();
            
            freq_uni.entrySet().forEach((lf) -> {
                Frequencia f = lf.getValue();
                lf.getValue().setTf_normalizad(f.getTf()/(double)totalUni);
            });
            
            freq_bi.entrySet().forEach((lf) -> {
                Frequencia f = lf.getValue();
                lf.getValue().setTf_normalizad(f.getTf()/(double)totalBi);
            });
            
            fromDB.setFreq_bigrama(freq_bi);
            fromDB.setFreq_unigrama(freq_uni);

//            for (Entry<Lexico, Frequencia> lf : freq_bi.entrySet()) {
//                System.out.println(lf.getKey() + " " + lf.getValue());
//            }
//            for (Entry<Lexico, Frequencia> lf : freq_uni.entrySet()) {
//                System.out.println(lf.getKey() + " " + lf.getValue());
//            }
//            System.out.println("\n\n");
            tweets_read++;
        }

        return datasetAnotado;
    }

    //Método que gera um dicionário com todos os usários do tweets coletados e refinados
    //com seus respectivos número de seguidores
    private static HashMap<String, Integer> buidUserRank(HashMap<String, Tweet> dataset) {
        HashMap<String, Integer> rank = new HashMap<>();

        for (Entry<String, Tweet> t : dataset.entrySet()) {
            User u = t.getValue().getUser();

            String id_user = u.getId();
            int followers_count = u.getFollowers();

            rank.put(id_user, followers_count);
        }

        return rank;
    }

    //Método para gerar as faixas de tweets de acordo com o número de seguidores
    private static HashMap<Integer, List<Tweet>> buildFaixas(HashMap<String, Tweet> dataset) {
        HashMap<Integer, List<Tweet>> faixas = new HashMap<>();

        int ultimaFaixa = 0;

        for (int i = 50; i <= 1700; i += 50) {
            List<Tweet> tweet_faixa = new ArrayList<>();
            for (Entry<String, Tweet> t : dataset.entrySet()) {
                int followers = t.getValue().getUser().getFollowers();
                if (followers > ultimaFaixa && followers <= i) {
                    tweet_faixa.add(t.getValue());
                }
            }
            //System.out.println("FAIXA " + ultimaFaixa + " - " + i + " TOTAL: " + tweet_faixa.size());
            ultimaFaixa = i;
            faixas.put(i, tweet_faixa);
        }

        List<Tweet> tweet_faixa = new ArrayList<>();
        for (Entry<String, Tweet> t : dataset.entrySet()) {
            int followers = t.getValue().getUser().getFollowers();
            if (followers > ultimaFaixa + 1) {
                tweet_faixa.add(t.getValue());
            }
        }
        //System.out.println("FAIXA " + ultimaFaixa + "+ TOTAL: " + tweet_faixa.size());
        faixas.put(ultimaFaixa + 1, tweet_faixa);

        return faixas;
    }

    //Método para gerar os arquivos contendo a % de tweets de cada faixa, definidas pelo professor Ely.
    private static void buildArquivosFaixas(HashMap<Integer, List<Tweet>> tweets_por_faixa, String mes) throws IOException {
        Random generator = new Random();
        int[] qtd_abs = {323, 204, 158, 119, 105, 90, 73, 60, 45, 46, 42, 39, 36, 28, 27, 28, 28, 18, 21, 20, 17, 14, 11, 17, 16, 10, 13, 8, 11, 10, 8, 8, 10, 6, 332};

        int faixa = 0;
        Map<Integer, List<Tweet>> treeMap = new TreeMap<>(tweets_por_faixa);

        FileWriter fwSec = new FileWriter(new File("distribuicao/" + mes + "/todasFaixas.csv"));
        BufferedWriter bwSec = new BufferedWriter(fwSec);
        bwSec.write("id;texto\n");

        for (Entry<Integer, List<Tweet>> tpf : treeMap.entrySet()) {
            int qtd_selec = qtd_abs[faixa];

            List<Tweet> tweets_da_faixa = tpf.getValue();
            List<Tweet> selecionados = new ArrayList<>();
            while (qtd_selec > 0) {
                selecionados.add(tweets_da_faixa.remove(generator.nextInt(tweets_da_faixa.size())));
                qtd_selec--;
            }

            FileWriter fw = new FileWriter(new File("distribuicao/" + mes + "/faixa" + tpf.getKey() + ".csv"));
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("id;texto\n");

            for (Tweet t : selecionados) {
                String toWrite = t.getId() + "; \"" + t.getText() + "\"\n";
                bw.write(toWrite);
                bwSec.write(toWrite);
            }

            bw.close();
            fw.close();
            faixa++;
        }

        bwSec.close();
        fwSec.close();
    }

    //Método para gerar o dataset composto por tweets oriundos de perfis públicos ou que contenham menções dele no texto.
    private static HashMap<String, Tweet> buildDataSetNews(HashMap<String, Tweet> dataset) {

        String[] perfis_noticias = {"@g1", "@UOL", "@folha", "@exame", "@Estadao", "@VEJA", "@portalR7", "UOLCiencia", "@folhadelondrina"};
        HashMap<String, Tweet> news = new HashMap<>();

        for (Entry<String, Tweet> t : dataset.entrySet()) {
            User u = t.getValue().getUser();
            Tweet tt = t.getValue();
            if (ArrayUtils.contains(perfis_noticias, ("@" + u.getName())) || stringContainsItemFromList(tt.getText(), perfis_noticias)) {
                news.put(tt.getId(), tt);
            }
        }

        return news;
    }

    //Métodos que geram um arquivo csv a partir de um dataset (mapeado).
    private static void geraCSV(HashMap<String, Tweet> dataset, String nome_arquivo) {
        try {
            FileWriter fw = new FileWriter(new File(nome_arquivo));
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("id;texto\n");

            for (Entry<String, Tweet> obj : dataset.entrySet()) {
                bw.write(obj.getKey() + "; \"" + obj.getValue().getText() + "\"\n");
            }

            bw.close();
            fw.close();

        } catch (IOException ex) {
            System.out.println("Não foi possível gerar o CSV.");
        }
    }

    private static void geraCSV2(HashMap<String, Integer> dataset, String nome_arquivo, boolean order) {

        try {
            FileWriter fw = new FileWriter(new File(nome_arquivo));
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("id_usuario;followers\n");

            HashMap<String, Integer> aux = sortByValue(dataset, order);
            for (Entry<String, Integer> obj : aux.entrySet()) {
                bw.write(obj.getKey() + "; \"" + obj.getValue() + "\"\n");
            }

            bw.close();
            fw.close();

        } catch (IOException ex) {
            System.out.println("Não foi possível gerar o CSV de rankqueamento.");
        }
    }

    private static HashMap<String, Integer> sortByValue(Map<String, Integer> unsortMap, final boolean order) {
        List<Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    //Método que gera o arquivo .CSV com os tweets para serem anotados;
    private static void selected2Anotate(String path_2anotar, HashMap<String, Tweet> dataset) throws FileNotFoundException, IOException {

        FileWriter fw = new FileWriter(new File("anotacao/paraAnotar.csv"));
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("id;texto;posicionamento\n");

        File folder = new File(path_2anotar);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {

                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);

                String line = br.readLine();
                line = br.readLine();

                while (line != null) {
                    String[] aux = line.split(",");
                    String id = aux[0].replace("\"", "");

                    if (aux[1].equals("1")) {
                        Tweet t = dataset.get(id);
                        bw.write("\"" + t.getId() + "\"; \"" + t.getText() + "\";\n");
                    }

                    line = br.readLine();
                }

                br.close();
                fr.close();
            }
        }

        bw.close();
        fw.close();
    }

    //Métodos para criar o Unigrama e Bigrama;    
    private static HashMap<String, HashMap<String, Integer>> buildUnigramaPolarizado(String path, List<Lexico> dicionarioLexico) throws FileNotFoundException, IOException {
        HashMap<String, HashMap<String, Integer>> unigrama = new HashMap<>();
        unigrama.put("POS", new HashMap<>());
        unigrama.put("NEG", new HashMap<>());
        unigrama.put("IRON", new HashMap<>());
        unigrama.put("OUT", new HashMap<>());

        List<String> verbosLig = new ArrayList<>();
        verbosLig.add("ser");
        verbosLig.add("estar");
        verbosLig.add("parecer");
        verbosLig.add("permanecer");
        verbosLig.add("ficar");
        verbosLig.add("continuar");
        verbosLig.add("andar");
        verbosLig.add("tornar");
        verbosLig.add("ano");
        verbosLig.add("nao");
        verbosLig.add("ter");
        verbosLig.add("mais");
        verbosLig.add("ir");
        verbosLig.add("ja");
        verbosLig.add("como");
        verbosLig.add("fazer");
        verbosLig.add("haver");
        verbosLig.add("poder");
        verbosLig.add("muito");
        verbosLig.add("so");

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {

                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);

                String line = br.readLine();
                line = br.readLine();

                int l_int = 1;
                while (line != null) {

                    String[] temp = line.split(";");
                    String id = temp[0];
                    String pol_anotada = temp[temp.length - 1];

                    StringBuilder text_tmp = new StringBuilder();
                    for (int i = 1; i <= temp.length - 2; i++) {
                        text_tmp.append(temp[i]).append(" ");
                    }

                    String text_final = StringUtils.normalizeSpace(text_tmp.toString().replace("\"", ""));

                    ComponentFactory factory = ComponentFactory.create(new Locale("pt", "BR"));
                    Analyzer cogroo = factory.createPipe();

                    Document document = new DocumentImpl();
                    document.setText(text_final);

                    cogroo.analyze(document);

                    HashMap<String, Integer> pal_freq = unigrama.get(pol_anotada);

                    document.getSentences().forEach((sentence) -> {
                        sentence.getTokens().forEach((token) -> {
                            for (String s : token.getLemmas()) {
                                String t_type = token.getPOSTag();
                                String sAcento = removerAcentos(s).toLowerCase();

                                if ((t_type.equals("n") || t_type.equals("prop") || t_type.equals("adj") || t_type.equals("adv")
                                        || t_type.equals("v-fin") || t_type.equals("v-inf") || t_type.equals("v-pcp") || t_type.equals("v-ger")) && !verbosLig.contains(sAcento)) {

                                    int polaridade = 1;
                                    char anot = 'M';

                                    for (Lexico aux : dicionarioLexico) {
                                        //Se o token atual possui sentimento, de acordo com o dic
                                        if (aux.getPalavra().equals(sAcento)) {
                                            anot = aux.getTipo_anotacao();

                                            switch (aux.getPolaridade()) {
                                                case -1:
                                                    polaridade = -2;
                                                    break;
                                                case 1:
                                                    polaridade = 2;
                                                    break;
                                                default:
                                                    polaridade = 1;
                                            }
                                            break;
                                        } //Se não foi encontrado o sentimento do token atual
                                    }

                                    //Lexico que representa um token anotado manualmente a polaridade;
                                    Lexico l = new Lexico(sAcento, t_type, anot, polaridade);

                                    //Adicionar o léxico à lista do tweet respectivo contabilizando sua frequencia;
                                    if (true) //                                    if (pal_freq.containsKey(sAcento)) {
                                    //                                        int freq = pal_freq.get(sAcento);
                                    //                                        pal_freq.put(sAcento, (freq + 1));
                                    //
                                    //                                    } else {
                                    //                                        pal_freq.put(sAcento, 1);
                                    //                                    }
                                    {

                                    }
                                }
                            }
                        });
                    });

                    unigrama.put(pol_anotada, pal_freq);

                    //System.out.println("ID:" + id + "\n" + "TEXTO: " + text_final + "\n" + "POLARIDADE: " + pol_anotada);
                    line = br.readLine();
                    //System.out.println(l_int);
                    l_int++;
                }
            }
        }
        return unigrama;
    }

    private static HashMap<String, HashMap<String, Integer>> buildBigramaPolarizado(String path) throws FileNotFoundException, IOException {
        HashMap<String, HashMap<String, Integer>> bigrama = new HashMap<>();
        bigrama.put("POS", new HashMap<>());
        bigrama.put("NEG", new HashMap<>());
        bigrama.put("IRON", new HashMap<>());
        bigrama.put("OUT", new HashMap<>());

        List<String> verbosLig = new ArrayList<>();
        verbosLig.add("ser");
        verbosLig.add("estar");
        verbosLig.add("parecer");
        verbosLig.add("permanecer");
        verbosLig.add("ficar");
        verbosLig.add("continuar");
        verbosLig.add("andar");
        verbosLig.add("tornar");
        verbosLig.add("ano");
        verbosLig.add("nao");
        verbosLig.add("ter");
        verbosLig.add("mais");
        verbosLig.add("ir");
        verbosLig.add("ja");
        verbosLig.add("como");
        verbosLig.add("fazer");
        verbosLig.add("haver");
        verbosLig.add("poder");
        verbosLig.add("muito");
        verbosLig.add("so");

        FileReader fr = new FileReader(new File(path));
        BufferedReader br = new BufferedReader(fr);

        String line = br.readLine();
        line = br.readLine();

        int l_int = 1;
        while (line != null) {

            String[] temp = line.split(";");
            String id = temp[0];
            String pol_anotada = temp[temp.length - 1];

            StringBuilder text_tmp = new StringBuilder();
            for (int i = 1; i <= temp.length - 2; i++) {
                text_tmp.append(temp[i]).append(" ");
            }

            String text_final = StringUtils.normalizeSpace(text_tmp.toString().replace("\"", ""));

            ComponentFactory factory = ComponentFactory.create(new Locale("pt", "BR"));
            Analyzer cogroo = factory.createPipe();

            Document document = new DocumentImpl();
            document.setText(text_final);

            cogroo.analyze(document);

            HashMap<String, Integer> pal_freq = bigrama.get(pol_anotada);

            document.getSentences().forEach((sentence) -> {
                sentence.getSyntacticChunks().forEach((structure) -> {
                    List<String> aux = new ArrayList<>();
                    structure.getTokens().forEach((token) -> {
                        for (String s : token.getLemmas()) {
                            String t_type = token.getPOSTag();
                            String sAcento = removerAcentos(s);

                            if ((t_type.equals("n") || t_type.equals("prop") || t_type.equals("adj") || t_type.equals("adv")
                                    || t_type.equals("v-fin") || t_type.equals("v-inf") || t_type.equals("v-pcp") || t_type.equals("v-ger")) && !verbosLig.contains(sAcento)) {

                                aux.add(removerAcentos(sAcento));
                            }
                        }
                    });

                    if (aux.size() > 1) {
                        for (int i = 0; i < aux.size() - 1; i = i + 2) {
                            StringBuilder bigr = new StringBuilder();
                            bigr.append(aux.get(i)).append(" ").append(aux.get(i + 1));
                            System.out.println(bigr.toString());

                            if (pal_freq.containsKey(bigr.toString())) {
                                int freq = pal_freq.get(bigr.toString());
                                pal_freq.put(bigr.toString(), (freq + 1));
                            } else {
                                pal_freq.put(bigr.toString(), 1);
                            }
                        }
                    }
                });
            });

            bigrama.put(pol_anotada, pal_freq);

            //System.out.println("ID:" + id + "\n" + "TEXTO: " + text_final + "\n" + "POLARIDADE: " + pol_anotada);
            line = br.readLine();
            //System.out.println(l_int);
            l_int++;
        }

        return bigrama;
    }

    //Métodos para criar os dicionários utilizados: internetes e lexico
    private static HashMap<String, Lexico> buildDicionarioLexico(String path) throws FileNotFoundException, IOException {
        HashMap<String, Lexico> dic = new HashMap<>();

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);

                String line = br.readLine();

                if (line.equals("op1")) {
                    line = br.readLine();

                    while (line != null) {
                        String[] aux = line.split(",");
                        String palavra = aux[0].toLowerCase();
                        Lexico l = new Lexico(palavra, aux[1], aux[3].charAt(0), Integer.parseInt(aux[2]));
                        dic.put(palavra, l);
                        line = br.readLine();
                    }
                } else if (line.equals("op2")) {
                    line = br.readLine();

                    while (line != null) {
                        String[] aux = line.split(";");
                        String palavra = StringUtils.strip(aux[0].toLowerCase(), ".");
                        String tag = aux[0].split("=")[1];
                        int polaridade = Integer.parseInt(aux[3].split("=")[1]);
                        char t_anot = aux[4].split("=")[1].charAt(0);

                        Lexico l = new Lexico(palavra, tag, t_anot, polaridade);
                        dic.put(palavra, l);
                        line = br.readLine();
                    }
                }

                br.close();
            }
        }

        return dic;
    }

    private static HashMap<String, String> buildDicionarioInternet(String path) throws FileNotFoundException, IOException {
        HashMap<String, String> dic = new HashMap<>();

        FileReader fr = new FileReader(new File(path));
        BufferedReader br = new BufferedReader(fr);

        String line = br.readLine();

        while (line != null) {
            String[] tokens = line.split(";");
            String abrev = tokens[0];
            String corresp = tokens[1];

            dic.put(abrev, corresp);
            line = br.readLine();
        }

        br.close();

        return dic;
    }

    //Métodos Auxiliares
    //Método Auxiliar para a remoção de acentos
    private static String removerAcentos(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    //Método Auxiliar para a remoção de urls em um determinado texto
    private static String removeUrl(String commentstr) {

        // rid of ? and & in urls since replaceAll can't deal with them
        String commentstr1 = commentstr.replaceAll("\\?", "").replaceAll("\\&", "").replaceAll("\\(", "").replaceAll("\\)", "");

        String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(commentstr1);
        int i = 0;
        while (m.find()) {
            if (m.group(i) != null) {
                commentstr = commentstr1.replaceAll(m.group(i), "").trim();
                i++;
            }
        }
        return commentstr;
    }

    //Método Auxiliar para verificar se uma string contém menção para algun dos perfis públicos
    private static boolean stringContainsItemFromList(String inputStr, String[] items) {
        return Arrays.stream(items).parallel().anyMatch(inputStr::contains);
    }

    //Método que constrói um map de ids anotados e suas polaridades, de acordo com os anotadores;
    private static HashMap<String, Integer> ids_anotadosManual(String path_a1, String path_a2, String path_a3) throws FileNotFoundException, IOException {
        HashMap<String, Integer> id_polarizacao = new HashMap<>();

        FileReader an_1 = new FileReader(new File(path_a1));
        FileReader an_2 = new FileReader(new File(path_a2));
        FileReader an_3 = new FileReader(new File(path_a3));

        BufferedReader b1 = new BufferedReader(an_1);
        BufferedReader b2 = new BufferedReader(an_2);
        BufferedReader b3 = new BufferedReader(an_3);

        String l1 = b1.readLine();
        String l2 = b2.readLine();
        String l3 = b3.readLine();

        while (l1 != null && l2 != null && l3 != null) {
            String[] aux1 = l1.split(";");
            String[] aux2 = l2.split(";");
            String[] aux3 = l3.split(";");

            String id = aux1[0];

            if (aux1.length > 2 && aux2.length > 2 && aux3.length > 2) {
                List<String> bag_pol = Arrays.asList(aux1[2], aux2[2], aux3[2]);
                String pol = maxOcorrencia(bag_pol);
                if (pol != null) {
                    id_polarizacao.put(id, Integer.parseInt(pol));

                    //Caso polaridade nula, onde os 3 anotadores discordaram;
                    //System.out.println(aux1[2] + " " + aux2[2] + " " + aux3[2] + " :" + id + "; " + aux1[1]);
                }

            }

            l1 = b1.readLine();
            l2 = b2.readLine();
            l3 = b3.readLine();
        }

        b1.close();
        b2.close();
        b3.close();

        return id_polarizacao;
    }

    //Método que retorna o número com a maior ocorrência dentro de um array;
    private static String maxOcorrencia(List<String> set) {
        String maiorOc = null;
        int maior = 0;

        HashMap<String, Integer> kappa = new HashMap<>();
        set.forEach((p) -> {
            if (kappa.containsKey(p)) {
                int fA = kappa.get(p);
                kappa.put(p, fA + 1);
            } else {
                kappa.put(p, 1);
            }
        });

        if (kappa.size() < 3) {
            for (Entry<String, Integer> e : kappa.entrySet()) {
                if (e.getValue() > maior) {
                    maior = e.getValue();
                    maiorOc = e.getKey();
                }
            }

        }

        return maiorOc;
    }
}
