package tcc.tcc_final;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.cogroo.analyzer.Analyzer;
import org.cogroo.analyzer.ComponentFactory;
import org.cogroo.text.Document;
import org.cogroo.text.impl.DocumentImpl;

public class MainApp extends Application {

    private static boolean ASC = true;
    private static boolean DESC = false;
    private static String mes = "setembro";

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        stage.setTitle("JavaFX and Maven");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        //launch(args);

        HashMap<String, String> dicionario = buildDicionarioInternet("dicionarios/dicionario_internetes.txt");
        //List<Lexico> lexico = buildDicionarioLexico("dicionarios/lexico_v3.txt");

//        HashMap<String, Tweet> dataset = buildDataSet("coletas/exp/" + mes, dicionario);
//        FileWriter fw = new FileWriter(new File(mes + ".txt"));
//        BufferedWriter bw = new BufferedWriter(fw);
//        bw.write("id\n");
//
//        for (Entry<String, Tweet> tc : dataset.entrySet()) {
//            bw.write(tc.getKey() + "\n");
//        }
//
//        System.out.println("done");
//        bw.close();
        //HashMap<String, Integer> rank = buidUserRank(dataset);
        //HashMap<Integer, List<Tweet>> tweets_por_faixa = buildFaixas(dataset);
        //buildArquivosFaixas(tweets_por_faixa, mes);
        //HashMap<String, Tweet> dataset_news = buildDataSetNews(dataset);
        //geraCSV(dataset, "datasets/" + mes + ".csv");
        //geraCSV2(rank, "ranks/" + mes + ".csv", DESC);
        //selected2Anotate("distribuicao/" + mes + "/todasFaixas.csv", dataset, 200);
        //System.out.println(dataset.size());
        HashMap<String, HashMap<String, Integer>> classes = buildUnigramaPolarizado("anotacao/anotados/");
        for (Entry<String, HashMap<String, Integer>> cl : classes.entrySet()) {
            System.out.println("CLASSE: " + cl.getKey());
            for (Entry<String, Integer> pf : cl.getValue().entrySet()) {
                System.out.println(pf.getKey() + ": " + pf.getValue());
            }

            System.out.println("\n");
        }
    }

    //Método que lê o arquivo .JSON e realiza a construção do dataset em memória do mesmo
    //utilizando a API Jackson
    private static HashMap<String, Tweet> buildDataSet(String path, HashMap<String, String> dic) throws IOException {
        HashMap<String, Tweet> tweets = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        File folder = new File(path);
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
                            text = StringUtils.normalizeSpace(text);
                            text = removeUrl(text);
                            text = text.replace("#", "");
                            text = text.replace("...", "");

                            String[] temp = text.toLowerCase().split(" ");
                            StringBuilder textoTratado = new StringBuilder(text.length());

                            for (String s : temp) {
                                if (dic.containsKey(s)) {
                                    textoTratado.append(dic.get(s));
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
                    System.out.println("Caminho não localizado: " + path);
                } catch (IOException ex) {
                    System.out.println("Não foi possível ler o arquivo");
                }
            }
        }

        return tweets;
    }

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

    //Método Auxiliar para verificar se uma string contém menção para algun dos perfis públicos.
    private static boolean stringContainsItemFromList(String inputStr, String[] items) {
        return Arrays.stream(items).parallel().anyMatch(inputStr::contains);
    }

    //Método que gera um arquivo csv a partir de um dataset (mapeado).
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

    //Método que gera um arquivo csv a partir de um dataset (mapeado).
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

    private static void selected2Anotate(String path, HashMap<String, Tweet> dataset, int qtd) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(new File(path));
        BufferedReader br = new BufferedReader(fr);

        List<String> ids = new ArrayList<>();

        String line = br.readLine();
        line = br.readLine();

        while (line != null) {
            String[] aux = line.split(";");
            String id = aux[0];
            ids.add(id);
            line = br.readLine();
        }

        br.close();
        fr.close();

        int anotadores = 3;

        for (int i = 1; i <= anotadores; i++) {
            int qtdAnotador = qtd / anotadores;

            FileWriter fw = new FileWriter(new File("anotacao/paraAnotar0" + i + ".csv"));
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("id;texto;anotacao\n");

            Random generator = new Random();

            while (qtdAnotador > 0) {
                int pos = generator.nextInt(ids.size());
                String id2write = ids.remove(pos);
                Tweet t = dataset.get(id2write);
                bw.write(t.getId() + "; \"" + t.getText() + "\";\" \"\n");
                qtdAnotador--;
            }

            bw.close();
            fw.close();
        }
    }

//    private static HashMap<String, String> buildDicionarioInternet(String path) throws FileNotFoundException, IOException {
//        HashMap<String, String> dic = new HashMap<>();
//
//        File fileDir = new File(path);
//        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileDir), "UTF8"));
//
//        FileWriter fw = new FileWriter(new File("dicionarios/dicionario_internetes.txt"));
//        BufferedWriter bw = new BufferedWriter(fw);
//
//        String line = br.readLine();
//
//        while (line != null) {
//            String[] tokens = line.split(" - ");
//            if (!line.equals("")) {
//
//                for (String s : tokens) {
//                    System.out.print(s + "\n");
//                }
//
//                String abrev = tokens[0].toLowerCase();
//                String corresp = tokens[1].toLowerCase();
//
//                if (corresp.contains("diminutivo de")) {
//                    corresp = corresp.replace("diminutivo de", "");
//                } else if (corresp.contains("diminutuvo de")) {
//                    corresp = corresp.replace("diminutuvo de", "");
//                } else if (corresp.contains("aumentativo de")) {
//                    corresp = corresp.replace("aumentativo de", "");
//                }
//
//                bw.write(abrev + ";" + corresp + "\n");
//                dic.put(abrev, corresp);
//            }
//            line = br.readLine();
//
//        }
//
//        br.close();
//        bw.close();
//
//        System.out.println(dic.size());
//        return dic;
//    }
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

    private static HashMap<String, HashMap<String, Integer>> buildUnigramaPolarizado(String path) throws FileNotFoundException, IOException {
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
                                String sAcento = removerAcentos(s);

                                if ((t_type.equals("n") || t_type.equals("prop") || t_type.equals("adj") || t_type.equals("adv")
                                        || t_type.equals("v-fin") || t_type.equals("v-inf") || t_type.equals("v-pcp") || t_type.equals("v-ger")) && !verbosLig.contains(sAcento)) {

                                    if (pal_freq.containsKey(sAcento)) {
                                        int freq = pal_freq.get(sAcento);
                                        pal_freq.put(sAcento, (freq + 1));

                                    } else {
                                        pal_freq.put(sAcento, 1);
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

    private static String removerAcentos(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    private static List<Lexico> buildDicionarioLexico(String path) throws FileNotFoundException, IOException {
        List<Lexico> dic = new ArrayList<>();

        FileReader fr = new FileReader(new File(path));
        BufferedReader br = new BufferedReader(fr);

        String line = br.readLine();

        while (line != null) {
            String[] aux = line.split(",");
            Lexico l = new Lexico(aux[0], aux[1], aux[3].charAt(0), Integer.parseInt(aux[2]));
            dic.add(l);
            line = br.readLine();
        }

        return dic;
    }

    private static void selected2AnotatePraias(String path, HashMap<String, Tweet> dataset, String mes) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(new File(path));
        BufferedReader br = new BufferedReader(fr);

        List<String> ids = new ArrayList<>();

        String line = br.readLine();
        line = br.readLine();

        while (line != null) {
            String[] aux = line.split(";");
            String id = aux[0];
            String selected = aux[1];
            if (selected.endsWith("1")) {
                ids.add(id);
            }
            line = br.readLine();
        }

        br.close();
        fr.close();

        for (String id : ids) {

            FileWriter fw = new FileWriter(new File("anotacao/paraAnotar_+" + mes + ".csv"));
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("id;texto;anotador1;anotador2;anotador3\n");

            bw.write(id + "; \"" + dataset.get(id).getText() + "\";\" \";\" \";\" \"\n");

            bw.close();
            fw.close();
        }
    }
}
