/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.luminance;

import java.io.IOException;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;

/**
 *
 * @author Sean
 */
public class OffsetTokenizer extends Tokenizer {
    private final CharTermAttribute charAttr = addAttribute(CharTermAttribute.class);;
    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncAttr = addAttribute(PositionIncrementAttribute.class);
    private final PositionLengthAttribute posLenAttr = addAttribute(PositionLengthAttribute.class);    
    private final PayloadAttribute payAttr = addAttribute(PayloadAttribute.class);
    private int index = 0;
    private int last_offset = -1;    
    
    public OffsetTokenizer(){          
    }

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        char[] chbuf = new char[1];
        StringBuilder sbbuf = new StringBuilder();
        boolean ignore_ch = true;
        int start_offset = index;
        int end_offset = 0;
        int len = 0;        
        while((len = input.read(chbuf)) > 0) {
            end_offset = index;
            switch(chbuf[0]){
                case '(':
                    ignore_ch = false;
                    break;
                case ')':
                    setup_token(sbbuf.toString(), start_offset, end_offset);
                    return true;
                case ' '|'\t'|'\n':
                    break;
                default:
                    if(ignore_ch) continue;
                    sbbuf.append(chbuf[0]);
                    break;
            }       
            index += 1;
        }
        
        return false;
    }
    
    private void setup_token(String strbuf, int so, int eo){
        String[] toks = strbuf.split(",");        
        int s_off = Integer.parseInt(toks[0].trim());
        int e_off = Integer.parseInt(toks[1].trim());             
        String tag = toks[2].trim();
        
        charAttr.setEmpty().append(tag);        
        offsetAttr.setOffset(s_off, e_off);
        if (s_off-last_offset != 0) {
            posIncAttr.setPositionIncrement(5);
        } else {
            posIncAttr.setPositionIncrement(1);
        }
        
        posLenAttr.setPositionLength(1);   
        last_offset = e_off;
    }
    
    @Override
    public void end() throws IOException {
        super.end();
        final int ofs = correctOffset(index);
        offsetAttr.setOffset(ofs, ofs);
        posLenAttr.setPositionLength(0);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {

        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        last_offset = -1;
        index = 0;
    }
}
