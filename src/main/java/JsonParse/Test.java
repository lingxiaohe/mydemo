package JsonParse;

import JsonParse.bean.TargetCityBean;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class Test {
    private String filePath;

    public Test() {
        filePath = this.getClass().getClassLoader().getResource("./source/china_city.js").getPath();
    }


    public static void main(String[] args) {
        Test test = new Test();
        test.readObjectFromFile();
    }

    private void readObjectFromFile() {
        File file = new File(filePath);
        InputStream inputStream;
        StringBuffer stringBuffer;
        List<Map<String, String>> cityData = null;//从源文件中获取到的城市信息
        try {
            inputStream = new FileInputStream(file);
            if (file.exists() && file.isFile()) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(inputStream, "utf-8"));
                stringBuffer = new StringBuffer();
                String line = null;
                while ((line = br.readLine()) != null) {
                    stringBuffer.append(line + "\n");
                }

                //截取不包含最外层括号的全部内容
                StringBuffer allStringBuffer = new StringBuffer(stringBuffer.substring(stringBuffer.indexOf("{") + 1, stringBuffer.lastIndexOf("}") - 1));
                cityData = new LinkedList<Map<String, String>>();
                //开始循环处理每层子括号里的内容
                while (-1 != allStringBuffer.indexOf("{")) {//当还有{存在，代表还有KV数据需要解析
                    //取出K
                    String key = allStringBuffer.substring(0, allStringBuffer.indexOf(":"));
                    //截取一个{
                    allStringBuffer = new StringBuffer(allStringBuffer.substring(stringBuffer.indexOf("{") + 1));//不包含边界括号
                    //截取到下一个}为止，作为V
                    HashMap<String, String> map = new HashMap<String, String>();
                    String value = allStringBuffer.substring(0, allStringBuffer.indexOf("}") - 1);
                    //先处理KV的异常符号
                    key = eraseSyntax(key).replace(",", "");//去掉key里面的","
                    value = eraseSyntax(value);
                    //放入map中
                    map.put(key, value);//不包括边界括号
                    cityData.add(map);
//                    System.out.println(map.toString());
                    allStringBuffer = new StringBuffer(allStringBuffer.substring(allStringBuffer.indexOf("}") + 1));
                }
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //开始进行数据处理
        //1.省级区划赋值

        Map<String, String> provinceData = cityData.get(0);
        List<Map> mapList = getMapValue(provinceData);
        ArrayList<TargetCityBean> cityBeanArrayList = new ArrayList<TargetCityBean>(mapList.size());
        //从第一个数组里取得关于各省的id、name
        for (int i = 0; i < mapList.size(); i++) {
            Set set = mapList.get(i).keySet();
            String key = null;
            Iterator iterator = set.iterator();
            String value = null;
            while (iterator.hasNext()) {
                key = (String) iterator.next();
                value = (String) mapList.get(i).get(key);
            }
            //对省级区划id、name赋值
            TargetCityBean targetCityBean = new TargetCityBean();
            targetCityBean.setId(key);//为省级区划给定id、name
            targetCityBean.setName(value);
            cityBeanArrayList.add(targetCityBean);
        }

        //2.为省级区划下的市级子类赋值
        for (int i = 0; i < cityBeanArrayList.size(); i++) {
            TargetCityBean provinceBean = cityBeanArrayList.get(i);//省级区划
            String provinceBeanId = provinceBean.getId();//省级区划编码
            List<TargetCityBean.CityListBeanX> cityList = new ArrayList<TargetCityBean.CityListBeanX>();//待赋值的该省级区划下的市级区划列表
            provinceBean.setCityList(cityList);
            for (int j = 1; j < cityData.size(); j++) {
                Map<String, String> map = cityData.get(j);//市级、区县的数组KV
                List<Map> cityListBeanXList = getMapValue(map);//取出一组城市列表
                String key = getKeyFromMap(map);//省级下面的行政区划编码
                if (null != provinceBeanId && !"".equals(key)) {
                    if (provinceBeanId.equals(key)) {//如果匹配上了，是该省的市级单位列表
                        TargetCityBean.CityListBeanX cityListBeanX;//新增一个市级区划
                        if (null != cityListBeanXList && 0 != cityListBeanXList.size()) {
                            for (int k = 0; k < cityListBeanXList.size(); k++) {
                                cityListBeanX = new TargetCityBean.CityListBeanX();
                                Map cityMap = cityListBeanXList.get(k);
                                String cityId = getKeyFromMap(cityMap);//市级单位id
                                String name = (String) cityMap.get(cityId);//市级名称
                                cityListBeanX.setId(cityId);
                                cityListBeanX.setName(name);
                                cityList.add(cityListBeanX);//加入
                                //3.进行区县级行政单位赋值
                                List<TargetCityBean.CityListBeanX.CityListBean> quXianList = new ArrayList<TargetCityBean.CityListBeanX.CityListBean>();//区县列表
                                cityListBeanX.setCityList(quXianList);
                                for (int l = 1; l < cityData.size(); l++) {
                                    Map<String, String> quXianMap = cityData.get(l);//寻找区县数据
                                    String quXianBelongCityId = getKeyFromMap(quXianMap);//市级id
                                    List<Map> quXianValueList = null;//该市级下的区县
//                                    System.out.println(quXianBelongCityId);

                                    if (null != quXianBelongCityId) {
                                        if (cityId.equals(quXianBelongCityId)) {
                                            quXianValueList = getMapValue(quXianMap);
                                            if (null != quXianValueList) {
                                                for (int m = 0; m < quXianValueList.size(); m++) {
                                                    Map quXianMap1 = quXianValueList.get(m);
                                                    Iterator iterator = quXianMap1.keySet().iterator();
                                                    String quXianId = null;//区县id
                                                    while (iterator.hasNext()) {
                                                        quXianId = (String) iterator.next();
                                                    }
                                                    String quXianValue = (String) quXianMap1.get(quXianId);
                                                    TargetCityBean.CityListBeanX.CityListBean quXianBean = new TargetCityBean.CityListBeanX.CityListBean();
                                                    quXianBean.setId(quXianId);//区县id
                                                    quXianBean.setName(quXianValue);//区县name
                                                    quXianList.add(quXianBean);//加入
                                                }
                                            }

                                        } else {//如果已经不是当前市的区划，就跳过
                                            if (0 > cityId.substring(0, 1).compareTo(quXianBelongCityId.substring(0, 1))) {
                                                if (quXianBelongCityId.length() == 2) {
                                                    continue;
                                                } else {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {//如果已经不是当前省的区划，就跳过
                        if (0 < key.substring(0, 1).compareTo(provinceBeanId.substring(0, 1))) {
                            break;
                        }
                    }
                }
            }
        }

        System.out.println("[");//起始方括号
        for (int i = 0; i < cityBeanArrayList.size(); i++) {
            TargetCityBean targetCityBean = cityBeanArrayList.get(i);
            String toJSONString = JSON.toJSONString(targetCityBean, SerializerFeature.WriteMapNullValue);
            if (i != cityBeanArrayList.size() - 1) {
                toJSONString += ",";
            }
            System.out.println(toJSONString);
        }
        System.out.println("]");//结束方括号
    }


    private String getKeyFromMap(Map<String, String> map) {
        Set<String> keySet = map.keySet();//取键对
        Iterator<String> iterator = keySet.iterator();//
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (null != key) {
                return key;
            }
        }
        return "";
    }

    private List<Map> getMapValue(Map<String, String> provinceData) {
        Set<String> strings = provinceData.keySet();
        Iterator<String> iterator = strings.iterator();
        String value = null;
        while (iterator.hasNext()) {
            value = provinceData.get(iterator.next());
        }
        List<Map> mapList = null;
        if (value != null) {
            String[] arrayList = value.split(",");
            if (0 != arrayList.length) {
                mapList = new ArrayList<Map>(arrayList.length);
                for (int i = 0; i < arrayList.length; i++) {
                    HashMap hashMap = new HashMap<String, String>();
                    String[] key_value = arrayList[i].split(":");
                    if (null == key_value || 2 > key_value.length) {
                        hashMap.put(key_value[0], "");
                        System.out.println("-----------null key or value----------" + key_value[0]);
                    } else {
                        hashMap.put(key_value[0], key_value[1]);
//                        System.out.println(key_value[0] + " : " + key_value[1]);
                    }
                    mapList.add(hashMap);
                }
            }
        }
        return mapList;
    }

    private String eraseSyntax(String s) {
        if (null != s) {
            //去除换行符、引号
            String replace;
            replace = s.replace(" ", "");//去除空格
            replace = replace.replace("\"", "");//去除引号
            replace = replace.replace("\n", "");//去除换行符
            return replace;
        } else
            return "";
    }
}
