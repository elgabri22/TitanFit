package com.example.titanfit.ui;

import com.example.titanfit.models.UserGoal;

public class Metodos {
    public static UserGoal calculaMacros(double peso, double altura,int edad,String genero, double factor_actividad, String objetivo){
        int res=0;
        UserGoal userGoal = null;
        if (genero.equalsIgnoreCase("Masculino")){
            res = (int) Math.round(10 * peso + 6.25 * altura - 5 * edad + 5);
        }else if (genero.equalsIgnoreCase("Femenino")){
            res = (int) Math.round(10 * peso + 6.25 * altura - 5 * edad - 161);
        }
        res=(int) Math.round(res*factor_actividad);
        if (objetivo.equalsIgnoreCase("PERDER GRASA")){
            int calorias=(int)Math.round(res*0.80);
            double proteinas=2.2*peso;
            double grasas=0.8*peso;
            double carbCalories = calorias - ((proteinas * 4) + (grasas * 9));
            double carbohidratos = carbCalories / 4.0;
            userGoal=new UserGoal(calorias,carbohidratos,proteinas,grasas,objetivo);
        }else if (objetivo.equalsIgnoreCase("MANTENER PESO")){
            int calorias=res;
            double proteinas=2.2*peso;
            double grasas=0.8*peso;
            double carbohidratos=calorias-((proteinas*4)+(grasas*9));
            userGoal=new UserGoal(calorias,carbohidratos,proteinas,grasas,objetivo);
        }else if (objetivo.equalsIgnoreCase("GANAR MASA MUSCULAR")){
            int calorias=res+((res*20)/100);
            double proteinas=2.2*peso;
            double grasas=0.8*peso;
            double carbohidratos=calorias-((proteinas*4)+(grasas*9));
            userGoal=new UserGoal(calorias,carbohidratos,proteinas,grasas,objetivo);
        }
        return userGoal;
    }
}
