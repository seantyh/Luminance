/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.luminance.corpus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import tw.com.kyle.luminance.AnnotationProvider;
import tw.com.kyle.luminance.LumIndexer;
import tw.com.kyle.luminance.LumDocument;

/**
 *
 * @author Sean
 */
public class PttJsonAdaptor implements LumIndexInterface {

    public class PttComment {
        public String author = "";
        public char valence = '0';
        public String comment = "";
    }
    
    public class PttArticle {
        public String author = "";
        public String title = "";
        public String postTime = "";
        public String content = "";
        public String url = "";
        public List<PttComment> comments = null;
    }
    
    
    private Logger logger = Logger.getLogger(PttJsonAdaptor.class.getName());
    public PttJsonAdaptor(){}       
    
    public List<PttArticle> Parse(String injsonpath) throws IOException{   
        if(!injsonpath.endsWith(".json")) return new ArrayList<>();
        String json_content = String.join("\n", Files.readAllLines(
                                Paths.get(injsonpath), StandardCharsets.UTF_8));        
        JsonElement root = new JsonParser().parse(json_content);
        JsonArray rarray = root.getAsJsonArray();
        List<PttArticle> art_list = new ArrayList<PttArticle>();
        for (JsonElement elem: rarray){
            JsonObject art_x = elem.getAsJsonObject();
            PttArticle art = new PttArticle();
            art.content = sanitize_ptt_content(art_x.get("content").getAsString());
            art.title = art_x.get("title").getAsString();
            art.author = art_x.get("author").getAsString();
            art.url = art_x.get("URL").getAsString();
            art.postTime = art_x.get("post_time").getAsString().substring(0, 8);
            if (!(art_x.get("comments") instanceof JsonNull)){
                art.comments = get_comments_from_json(art_x.getAsJsonObject("comments"));                        
            } else {
                art.comments = new ArrayList<>();
            }
        
            art_list.add(art);
        }
        
        logger.info(String.format("Processing %d PttArticle in %s", art_list.size(), injsonpath));
        return art_list;
    }
    
    private List<PttComment> get_comments_from_json(JsonObject com_json){
        JsonArray push_arr = com_json.getAsJsonArray("push");
        JsonArray boo_arr = com_json.getAsJsonArray("boo");
        JsonArray arrow_arr = com_json.getAsJsonArray("arrow");
        
        class Extract_comment implements Function<JsonArray, List<PttComment>>{
            private char valence = 0;
            public Extract_comment(char val) {
                valence = val;
            }
            @Override
            public List<PttComment> apply(JsonArray jarr) {
                List<PttComment> com_list = new ArrayList<>();
                for(JsonElement com: jarr){
                    JsonArray com_arr_x = com.getAsJsonArray();
                    PttComment com_obj = new PttComment();
                    com_obj.valence = valence;
                    com_obj.comment = com_arr_x.get(1).getAsString();
                    com_obj.author = com_arr_x.get(0).getAsString();                    
                    com_list.add(com_obj);     
                }
                return com_list;
            }
        }
                
        List<PttComment> comments = new ArrayList<>();
        comments.addAll(new Extract_comment('+').apply(push_arr));
        comments.addAll(new Extract_comment('-').apply(boo_arr));
        comments.addAll(new Extract_comment('=').apply(arrow_arr));
        return comments;
    }
    
    @Override
    public void Index(LumIndexer indexer, String inpath) throws IOException{        
        List<PttArticle> art_list = Parse(inpath);
        boolean IS_PLAIN_TEXT = true;
        
        if(art_list == null || art_list.isEmpty()) {
            logger.warning("Failed to load for index " + inpath);
            return;
        }
        
        for(PttArticle art: art_list){
            AnnotationProvider annot_prov = new AnnotationProvider(art.content, IS_PLAIN_TEXT);
            annot_prov.AddSupplementData("author", art.author);
            annot_prov.AddSupplementData("title", art.title);
            annot_prov.SetBaseTimestamp(art.postTime);            
            annot_prov.AddSupplementData("url", art.url);                        
                                                
            for(PttComment com: art.comments){
                LumDocument frag = new LumDocument();
                frag.SetBaseRef(annot_prov.GetRefUuid());
                frag.SetDocMode(LumDocument.FRAG); 
                frag.SetDocClass(LumDocument.DISCOURSE);
                frag.SetTimestamp(art.postTime);
                frag.AddSuppData("author", com.author);
                frag.AddSuppData("valence", String.valueOf(com.valence));                
                frag.SetContent(com.comment);
                annot_prov.AddLumDocument(frag);
            }                                                        
            annot_prov.Index(indexer);
        }
                                       
    }
    
    private String lucene_date_format(String timestamp) {                    
        String lum_date_str = null;
        try {
            DateFormat format = new SimpleDateFormat("yyyyMMdd");
            lum_date_str = DateTools.dateToString(
                                    format.parse(timestamp),
                                    Resolution.DAY);            
        } catch (ParseException ex) { 
            Calendar cal = Calendar.getInstance();
            cal.set(2010, 1, 1, 0, 0, 0);
            lum_date_str = DateTools.dateToString(
                                    cal.getTime(), Resolution.DAY);            
        }
        
        return lum_date_str;                
    }
    
    private String sanitize_ptt_content(String raw){
        String[] lines = raw.split("\n");
        StringBuilder sb = new StringBuilder();
        for(String ln: lines) {
            if (ln.startsWith(":"))
                continue;
            sb.append(ln); // sb.append("\n");
        }
        
        return sb.toString();
    }
}
