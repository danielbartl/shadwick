import test from 'node:test';
import assert from 'node:assert/strict';
import {javaApi} from './java-api.mjs';

test('direct declarations include constructors and typed options but omit private and anonymous members',()=>{
 const api=javaApi(`public class Example<T> {
 /** Configure its value. @return this */
 public Example<T> setValue(IModel<T> value) { return this; }
 public Example(String id) {}
 private void hidden() {}
 private Object callback = new Object() { public void internal() {} };
 public static final String DATA = "public void fake() { }";
 public enum Size { SMALL("sm"), DEFAULT(null); Size(String value) {} }
 protected void onSubmit(AjaxRequestTarget target) {}
 @Override public void onConfigure() {}
 }`);
 assert.equal(api.name,'Example');assert.deepEqual(api.members.map(m=>m.kind),['Method','Constructor','Enum','Hook']);
 assert.equal(api.members[2].description,'SMALL, DEFAULT');assert(api.members[0].description.includes('Configure its value.'));
 assert(!api.members.some(m=>m.signature.includes('internal')||m.signature.includes('fake')||m.signature.includes('onConfigure')));
});
test('braces and parentheses in comments or strings do not disturb signatures',()=>{
 const api=javaApi(`/** Mention public class Wrong { } */ public final class Real {
 private String text="braces { } and )";
 /** Runs a callback. */
 public <T> Real onDone(SerializableBiConsumer<T, List<String>> callback,
 IModel<String> message) { return this; }
 }`);
 assert.equal(api.name,'Real');assert.equal(api.members.length,1);assert.equal(api.members[0].signature,'public <T> Real onDone(SerializableBiConsumer<T, List<String>> callback, IModel<String> message)');
});
test('implicit interface methods and record constructors are exposed',()=>{
 const api=javaApi('public interface Store extends Serializable { /** Read. */ Skin load(); void save(Skin skin); }');
 assert.deepEqual(api.members.map(m=>m.signature),['Skin load()','void save(Skin skin)']);
 const record=javaApi('public record Range(LocalDate start, LocalDate end) { public Range { } public boolean empty() { return false; } }');
 assert.equal(record.name,'Range');assert.equal(record.members[0].kind,'Record');assert.equal(record.members[1].signature,'public boolean empty()');
});
test('nested helper factories do not become methods of the outer class',()=>{
 const api=javaApi('public class Facade { public static class Helpers { public static void nested() {} } public static void toast(String message) {} }');
 assert.deepEqual(api.members.map(m=>m.signature),['public static void toast(String message)']);
});
test('package-private implementation helpers cannot be presented as public API',()=>assert.throws(()=>javaApi('final class Internal { public void run() {} }'),/public Java API/));

test('root enums expose values and class declarations preserve inheritance',()=>{
 const api=javaApi('public enum Mode { LIGHT, DARK, SYSTEM }');assert.equal(api.members[0].signature,'LIGHT, DARK, SYSTEM');
 const component=javaApi('public class Input<T> extends Base<T> { public Input(String id) {} }');assert.equal(component.declaration,'public class Input<T> extends Base<T>');
});
