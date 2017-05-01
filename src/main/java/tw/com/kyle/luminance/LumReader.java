/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.luminance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author Sean
 */
public class LumReader {
    private IndexReader reader = null;
    private IndexSearcher searcher = null;
    public LumReader(IndexReader r) {
        reader = r;
        searcher = new IndexSearcher(reader);
    }
    
    public Document getDocument(long uuid) throws IOException {
        BytesRef bref = LumUtils.LongToBytesRef(uuid);
        TermQuery query = new TermQuery(new Term("uuid", bref));
        TopDocs docs = searcher.search(query, 1); 
        if (docs.totalHits > 0){
            return searcher.doc(docs.scoreDocs[0].doc);
        } else {
            return null;
        }    
    }
        
    public int getDocId(Document doc) throws IOException {
        BytesRef bref = doc.getBinaryValue("uuid");
        TermQuery q = new TermQuery(new Term("uuid", bref));
        TopDocs docs = searcher.search(q, 1);
        if (docs.totalHits > 0) {
            return docs.scoreDocs[0].doc;
        } else {
            return -1;
        }
    }
    
    public List<Long> getAnnotations(long ref_uuid) throws IOException {
        Document ref_doc = getDocument(ref_uuid);
        if (ref_doc == null) return null;
        if (!ref_doc.get("type").equals(LumIndexer.DOC_DISCOURSE)) return null;
        
        List<Long> uuid_list = new ArrayList<>();
        BytesRef bref = LumUtils.LongToBytesRef(ref_uuid);
        TermQuery tquery = new TermQuery(new Term("base_ref", bref));        
        searcher.search(tquery, new SimpleCollector() {
            @Override
            public void collect(int i) throws IOException {
                BytesRef uuid_x = reader.document(i).getBinaryValue("uuid");
                uuid_list.add(LumUtils.BytesRefToLong(uuid_x));
                System.out.printf("%016x%n", uuid_list.get(uuid_list.size()-1));
            }

            @Override
            public boolean needsScores() {
                return false;
            }
            
        });
        
        return uuid_list;
    }
        
        
}
