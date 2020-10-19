package solaris;

public class Data {
    private String ikey;
    private Integer ivalue;

    public Data(String ikey, int ivalue) {
        this.setIkey(ikey);
        this.ivalue = ivalue;
    }

    public Integer getIvalue() {
        return ivalue;
    }

    public void setIvalue(Integer ivalue) {
        this.ivalue = ivalue;
    }

    public String getIkey() {
        return ikey;
    }

    public void setIkey(String ikey) {
        this.ikey = ikey;
    }
}