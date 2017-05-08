/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.luminance.test;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.junit.Ignore;
import org.junit.Test;

import tw.com.kyle.luminance.LumReader;
import tw.com.kyle.luminance.LumWindow;
import tw.com.kyle.luminance.Luminance;

/**
 *
 * @author Sean_S325
 */
public class LumWindowTest {

    private String INDEX_DIR = "h:/index_dir";    
    
    private void setup() throws IOException {
        try {
            Luminance.clean_index(INDEX_DIR);
        } catch (IOException ex){
            System.out.println(ex);
            fail(ex.toString());
        }
        Luminance lum = new Luminance(INDEX_DIR);
        String txt = String.join("",
                Files.readAllLines(Paths.get("etc/test/simple_text.txt"), StandardCharsets.UTF_8));
        JsonObject elem = (JsonObject) lum.add_document(txt);
        lum.close();
    }

    
    @Test
    public void testInstantiation() {
        try {
            setup();
            LumReader lum_reader = new LumReader(INDEX_DIR);
            IndexReader reader = lum_reader.GetReader();
            Document targ_doc = reader.document(0);
            LumWindow lumWin = new LumWindow();
            lumWin.initialize(targ_doc, reader);
            lum_reader.close();
        } catch (IOException ex) {
            Logger.getLogger(LumWindowTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IOException thrown");
        }
    }

    @Test
    public void testDiscourseWindow() {
        try {
            setup();
            
            LumReader lum_reader = new LumReader(INDEX_DIR);
            IndexReader reader = lum_reader.GetReader();
            Document targ_doc = reader.document(0);
            LumWindow lumWin = new LumWindow();
            lumWin.initialize(targ_doc, reader);
            
            String ret = lumWin.GetWindow(5, 10, 11);
            assertTrue(ret.equals("意義且可以 自 由使用的語"));
            lum_reader.close();
        } catch (IOException ex) {
            Logger.getLogger(LumWindowTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IOException thrown");
        }
    }

    @Ignore @Test
    public void testAnnotationWindow(){
        try {
            setup();
            
            LumReader lum_reader = new LumReader(INDEX_DIR);
            IndexReader reader = lum_reader.GetReader();
            Document targ_doc = reader.document(1);
            LumWindow lumWin = new LumWindow();
            lumWin.initialize(targ_doc, reader);
            
            String ret = lumWin.GetWindow(5, 5, 6);
            assertTrue(ret.equals("詞是最小有 意義 且可以自由"));
            lum_reader.close();
        } catch (IOException ex) {
            Logger.getLogger(LumWindowTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IOException thrown");
        }
    }
    
    @Ignore
    @Test
    public void testReconstruct() throws IOException {
        setup();
        LumReader lum_reader = new LumReader(INDEX_DIR);
        IndexReader reader = lum_reader.GetReader();
        Document targ_doc = reader.document(1);
        LumWindow lumWin = new LumWindow();
        lumWin.initialize(targ_doc, reader);

        String ret = lumWin.Reconstruct(5, 5, 6);

    }
}
