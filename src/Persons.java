public abstract class Persons {
    protected String name, contactNumber;
    protected int age ;

    public Persons() {
        this.name = "";
        this.age = 0;
        this.contactNumber= "";
    }

    public Persons(String name){
        this();
        this.name = name;
    }

    public Persons(String name, int age) {
        this(name);
        this.age = age;
    }

    public Persons(String name, int age, String contactNumber) {
        this(name, age);
        this.contactNumber = contactNumber;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String displayInfo() {
        return "Name: " + name + " | Age: " + age + " | Contact Number: " + contactNumber;
    }

    @Override
    public String toString() {
        return displayInfo();
    }
}