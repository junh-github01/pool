package cn.jun.thread.exception;

public class DiscardException  extends RuntimeException{
    public DiscardException(){
    }

    public DiscardException(String message){
        super(message);
    }
}