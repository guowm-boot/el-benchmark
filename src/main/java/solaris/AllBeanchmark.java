package solaris;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.StringTemplateResourceLoader;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class AllBeanchmark {

    private Map<String, Object> paras = new HashMap<String, Object>();

    private GroupTemplate gt;

    private Invocable arithInv;
    private Invocable objectInv;
    private Invocable condInv;

    private Expression arithExp;
    private Expression objectExp;
    private Expression condExp;

    @Setup
    public void init() {
        paras.put("A", new Data("false", 23342423));
        paras.put("B", new Data("false", 435454));
        paras.put("C", new Data("true", 121243));
        paras.put("D", new Data("false", 23));

        initScript();
        initBeetl();
        initAviator();
    }

    @Benchmark
    public void testArith() throws Exception {
        Object result = (((Data)paras.get("A")).getIvalue()+((Data)paras.get("B")).getIvalue()-((Data)paras.get("C")).getIvalue())*((Data)paras.get("D")).getIvalue();
    }

    @Benchmark
    public void testObject() throws Exception {
        Map<String, Object> result = new HashMap<String, Object>(4);
        result.put("f1", ((Data)paras.get("A")).getIvalue());
        result.put("f2", ((Data)paras.get("A")).getIvalue()+((Data)paras.get("B")).getIvalue());
        result.put("f3", ((Data)paras.get("C")).getIvalue());
        result.put("f4", (((Data)paras.get("A")).getIvalue()+((Data)paras.get("B")).getIvalue()-((Data)paras.get("C")).getIvalue())*((Data)paras.get("D")).getIvalue());
    }

    @Benchmark
    public void testCond() throws Exception {
        Object result = 0;
        if (((Data)paras.get("A")).getIkey().equals("true")) {
            result = ((Data)paras.get("A")).getIvalue();
        } else if (((Data)paras.get("B")).getIkey().equals("true")) {
            result = ((Data)paras.get("B")).getIvalue();
        } else if (((Data)paras.get("C")).getIkey().equals("true")) {
            result = ((Data)paras.get("C")).getIvalue();
        } else if (((Data)paras.get("D")).getIkey().equals("true")) {
            result = ((Data)paras.get("D")).getIvalue();
        }
    }

    @Benchmark
    public void testArithByScript() throws Exception {
        Object result = arithInv.invokeFunction("testArith", paras);
    }

    @Benchmark
    public void testObjectByScript() throws Exception {
        Object result = objectInv.invokeFunction("testObject", paras);
    }

    @Benchmark
    public void testCondByScript() throws Exception {
        Object result = condInv.invokeFunction("testCond", paras);
    }

    @Benchmark
    public void testArithByBeetl() {
        Map result = gt.runScript("return (A.ivalue+B.ivalue-C.ivalue)*D.ivalue;", paras);
    }

    @Benchmark
    public void testObjectByBeetl() {
        Map result = gt.runScript("var object = {f1: A.ivalue, f2: A.ivalue+B.ivalue, f3: C.ivalue, f4: (A.ivalue+B.ivalue-C.ivalue)*D.ivalue}; ", paras);
    }

    @Benchmark
    public void testCondByBeetl() {
        Map result = gt.runScript("if(A.ikey=='true'){return A.ivalue;}else if(B.ikey=='true'){return B.ivalue;}else if(C.ikey=='true'){return C.ivalue;}else if(D.ikey=='true'){return D.ivalue;}else{return 0;}", paras);
    }

    @Benchmark
    public void testArithByAviator() {
        Object result = arithExp.execute(paras);
    }

    @Benchmark
    public void testObjectByAviator() {
        Object result = objectExp.execute(paras);
    }

    @Benchmark
    public void testCondByAviator() {
        Object result = condExp.execute(paras);
    }

    private void initScript() {
        ScriptEngineManager manager = new ScriptEngineManager();
        try {
            ScriptEngine engine = manager.getEngineByName("js");
            engine.eval("function testArith(paras){return (paras.A.ivalue+paras.B.ivalue-paras.C.ivalue)*paras.D.ivalue;}");
            arithInv = (Invocable) engine;

            engine = manager.getEngineByName("js");
            engine.eval("function testObject(paras){var object={f1: paras.A.ivalue, f2: paras.A.ivalue+paras.B.ivalue, f3: paras.C.ivalue, f4: (paras.A.ivalue+paras.B.ivalue-paras.C.ivalue)*paras.D.ivalue}; return object;}");
            objectInv = (Invocable) engine;

            engine = manager.getEngineByName("js");
            engine.eval("function testCond(paras){if(paras.A.ikey=='true'){return paras.A.ivalue;}else if(paras.B.ikey=='true'){return paras.B.ivalue;}else if(paras.C.ikey=='true'){return paras.C.ivalue;}else if(paras.D.ikey=='true'){return paras.D.ivalue;}else{return 0;}}");
            condInv = (Invocable) engine;
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        System.out.println("JS准备工作就绪！");
    }

    private void initBeetl() {
        try {
            Configuration cfg = Configuration.defaultConfiguration();
            gt = new GroupTemplate(new StringTemplateResourceLoader(), cfg);
        } catch (IOException e) {
            throw new RuntimeException("初始化Beetl资源加载器失败", e);
        }
        System.out.println("Beetl准备工作就绪！");
    }

    private void initAviator() {
        arithExp = AviatorEvaluator.getInstance().compile("return (A.ivalue+B.ivalue-C.ivalue)*D.ivalue;");
        objectExp = AviatorEvaluator.getInstance().compile("let object=seq.map('f1', A.ivalue, 'f2', A.ivalue+B.ivalue, 'f3', C.ivalue, 'f4', (A.ivalue+B.ivalue-C.ivalue)*D.ivalue); return object;");
        condExp = AviatorEvaluator.getInstance().compile("if(A.ikey=='true'){return A.ivalue;}elsif(B.ikey=='true'){return B.ivalue;}elsif(C.ikey=='true'){return C.ivalue;}elsif(D.ikey=='true'){return D.ivalue;}else{return 0;}");
        System.out.println("Aviator准备工作就绪！");
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder().include(AllBeanchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }

}